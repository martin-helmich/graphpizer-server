package persistence

import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import play.api.Logger

trait BackendInterface {

  def createNode: Node

  def createLabel(name: String): Label

  def transactional[T](func: (BackendInterface, Transaction) => T): T

  def execute(cypher: String): Result

  def shutdown()

}

class Backend(graph: GraphDatabaseService) extends BackendInterface {

  def createNode = graph.createNode()

  def createLabel(name: String) = DynamicLabel label name

  def transactional[T](func: (BackendInterface, Transaction) => T): T = {
    val tx = graph.beginTx()
    try {
      Logger.info("Starting Neo4j transaction")
      val result = func(this, tx)
      tx.success()
      Logger.info("Transaction success")
      return result
    } catch {
      case e: Exception =>
        Logger.error(e.getMessage)
        tx.failure()
        throw e
    } finally {
      tx.close()
    }
  }

  def execute(cypher: String) = {
    graph.execute(cypher)
  }

  def shutdown() = graph.shutdown()

}
