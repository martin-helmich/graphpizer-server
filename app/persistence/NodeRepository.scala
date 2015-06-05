package persistence

import org.neo4j.graphdb.Node

class NodeRepository(backend: BackendInterface) {

  def find(q: Query): Option[Node] = {
    val pf = propertyFilter(q)

    val cypher = s"""MATCH (n:${q.label.name} {$pf}) RETURN n LIMIT 1"""
    val result = q.properties match {
      case null => backend.execute(cypher).run()
      case m: Map[String, Any] => backend.execute(cypher).runWith(Map("props" -> q.properties))
    }

    if (result.hasNext) {
      val node = result.columnAs[Node]("n").next()
      result.close()
      Some(node)
    } else {
      None
    }
  }

  def merge(q: Query): Node = {
    val pf = propertyFilter(q)

    val cypher = s"""MERGE (n:${q.label.name} {$pf}) RETURN n"""
    val result = q.properties match {
      case null => backend.execute(cypher).run()
      case m: Map[String, Any] => backend.execute(cypher).runWith(Map("props" -> q.properties))
    }

    val node = result.columnAs[Node]("n").next()
    result.close()
    return node
  }

  protected def propertyFilter(q: Query) = q.properties match {
    case null => ""
    case map: Map[String, Any] => map map { case (key, _) => s"$key: {props}.$key" } mkString ", "
  }

}
