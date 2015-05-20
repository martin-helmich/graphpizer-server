package persistence

import org.neo4j.graphdb.{Result, GraphDatabaseService}

class Statement(graph: GraphDatabaseService, cypher: String) {

  def run: Result = graph.execute(cypher)
  def runWith(params: Map[String, AnyRef]):Result = graph.execute(cypher, params)

}
