package persistence

import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.traversal.TraversalDescription
import play.api.Logger

trait BackendInterface {

  def createNode: Node

  def createLabel(name: String): Label

  def createEdgeType(name: String): RelationshipType

  def transactional[T](func: (BackendInterface, Transaction) => T): T

  def execute(cypher: String): Statement

  def traversal: TraversalDescription

  def shutdown()

}

class Backend(graph: GraphDatabaseService) extends BackendInterface {

  def createNode = graph.createNode()

  def createLabel(name: String) = DynamicLabel label name

  def createEdgeType(name: String) = DynamicRelationshipType withName name

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
        Logger.error(e.getMessage, e)
        tx.failure()
        throw e
    } finally {
      tx.close()
    }
  }

  def traversal: TraversalDescription = graph.traversalDescription()

  def execute(cypher: String) = { new Statement(graph, cypher) }

  def shutdown() = graph.shutdown()

}
