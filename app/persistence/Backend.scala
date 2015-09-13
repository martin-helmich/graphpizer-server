package persistence

/**
 * GraPHPizer source code analytics engine
 * Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
  def transactionalDebug[T](func: (BackendInterface, Transaction) => T): T

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
  def transactionalDebug[T](func: (BackendInterface, Transaction) => T): T = {
    Logger.info("Starting transaction")
    val tx = graph.beginTx()
    try {
      Logger.info("Evaluation function")
      val result = func(this, tx)
      tx.success()
      Logger.info("Success")
      result
    } catch {
      case e: Exception =>
        Logger.error(e.getMessage, e)
        tx.failure()
        Logger.error("Error", e)
        throw e
    } finally {
      Logger.info("closing")
      tx.close()
      Logger.info("closed")
    }
  }

  def traversal: TraversalDescription = graph.traversalDescription()

  def execute(cypher: String) = { new Statement(graph, cypher) }

  def shutdown() = graph.shutdown()

}
