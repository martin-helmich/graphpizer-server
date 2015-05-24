package persistence

import org.neo4j.graphdb.Node

class NodeRepository(backend: BackendInterface) {

  def merge(q: Query): Node = {
    val propertyFilter = q.properties match {
      case null => ""
      case map: Map[String, Any] => map map { case (key, _) => s"$key: {props}.$key" } mkString ", "
    }

    val cypher = s"""MERGE (n:${q.label.name} {$propertyFilter}) RETURN n"""
    val result = q.properties match {
      case null => backend.execute(cypher).run()
      case m: Map[String, Any] => backend.execute(cypher).runWith(Map("props" -> q.properties))
    }

    val node = result.columnAs[Node]("n").next()
    result.close()
    return node
  }

}
