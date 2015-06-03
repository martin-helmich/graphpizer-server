package persistence

import org.neo4j.graphdb._
import org.neo4j.graphdb.traversal.TraversalDescription
import play.api.Logger

trait BackendInterface {

  def nodes: NodeRepository

  def createNode(labels: String*): Node

  def createNode(label: Label, moreLabels: Label*): Node

  def createLabel(name: String): Label

  def createEdgeType(name: String): RelationshipType

  def transactional[T](func: (BackendInterface, Transaction) => T): T

  def execute(cypher: String): Statement

  def traversal: TraversalDescription

  def shutdown()

}

class Backend(graph: GraphDatabaseService) extends BackendInterface {

  protected val nodeRepository = new NodeRepository(this)

  def createNode(labelNames: String*): Node = {
    val node = graph.createNode()
    labelNames map { createLabel } foreach { node.addLabel }
    return node
  }

  def createNode(label: Label, moreLabels: Label*): Node = {
    val node = graph.createNode(label)
    moreLabels foreach { node.addLabel }
    return node
  }

  def nodes = { nodeRepository }

  def createLabel(name: String) = DynamicLabel label name

  def createEdgeType(name: String) = DynamicRelationshipType withName name

  def transactional[T](func: (BackendInterface, Transaction) => T): T = {
    val tx = graph.beginTx()
    try {
      val result = func(this, tx)
      tx.success()
      result
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
