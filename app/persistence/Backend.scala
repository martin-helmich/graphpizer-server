package persistence

import org.neo4j.graphdb.{Transaction, DynamicLabel, Label, Node}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import play.api.Logger

trait Backend {

  def createNode: Node

  def createLabel(name: String): Label

  def transactional(func: (Transaction) => Unit)

  def shutdown

}

object Backend extends Backend {

  val graph = new GraphDatabaseFactory() newEmbeddedDatabase "/tmp/graphizer"

  def createNode = graph.createNode()

  def createLabel(name: String) = DynamicLabel label name

  def transactional(func: (Transaction) => Unit) = {
    val tx = graph beginTx()
    try {
      Logger.info("Starting Neo4j transaction")
      func(tx)
      tx.success()
      Logger.info("Transaction success")
    } catch {
      case e: Exception =>
        Logger.error(e.getMessage)
        tx.failure()
    }
  }

  def shutdown = graph.shutdown()

}
