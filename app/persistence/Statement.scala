package persistence

import org.neo4j.graphdb.{Result, GraphDatabaseService}

import scala.collection.JavaConversions._

class Statement(graph: GraphDatabaseService, cypher: String) {

  def run: Result = graph.execute(cypher)
  def runWith(params: Map[String, AnyRef]):Result = graph.execute(cypher, mapAsJavaMap[String, AnyRef](params))

}
