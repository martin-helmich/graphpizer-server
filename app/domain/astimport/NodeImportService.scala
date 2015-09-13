package domain.astimport

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

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import controllers.dto.ImportDataSet
import domain.model.AstNodeTypes._
import domain.model.ModelEdgeTypes._
import org.neo4j.graphdb
import org.neo4j.graphdb._
import persistence.{Query, BackendInterface, ConnectionManager}
import persistence.NodeWrappers._
import util.WrappingActorLogging
import domain.model.ModelLabelTypes._

import scala.collection.JavaConversions._

class NodeImportService(manager: ConnectionManager) extends Actor with WrappingActorLogging with ActorLogging {

  import NodeImportService._

  var types = new Types()

  def receive = LoggingReceive {
    case ImportRequest(project, data) => importData(project, data)
    case WipeRequest(project) =>
      wipe(project)
      sender() ! WipeSuccess()
    case AllClear() => sender() ! AllClear()
    case unknown => log.warning("Unknown request to actor: " + unknown)
  }

  def wipe = (project: String) => withLog(s"Wiping all nodes on project $project") exec {
    manager connect project transactional { (b, t) =>
      b.execute("MATCH (c) OPTIONAL MATCH (c)-[r]->() DELETE c, r").run()
    }
  }

  def importData = (project: String, data: ImportDataSet) => withLog(s"Importing ${data.nodes.size } nodes.") exec {
    manager connect project transactional { (b, t) =>
      buildPrimitiveDataTypes(b)

      val knownNodes = data.nodes.map { dto =>
        dto.merge match {
          case Some(true) =>
            val labels = dto.labels mkString ":"
            val properties = dto.properties map { case (key, value) => key + ": {node}." + key } mkString ", "
            val params = Map("node" -> dto.properties)

            val res = b execute s"MERGE (n:$labels  {$properties}) RETURN n" runWith params

            val nodes: Iterator[graphdb.Node] = res.columnAs[graphdb.Node]("n")
            val arr = nodes.toArray
            (dto.id, arr(0))
          case _ =>
            val node = b.createNode()

            dto.labels.foreach { l => node.addLabel(b createLabel l) }
            dto.properties.foreach { case (key, value) => node(key) = value }

            val possibleType = dto.labels.head match {
              case "Scalar_LNumber" |
                   "Expr_BinaryOp_BitwiseAnd" |
                   "Expr_BinaryOp_BitwiseOr" |
                   "Expr_BinaryOp_BitwiseXor" |
                   "Expr_BinaryOp_ShiftLeft" |
                   "Expr_BinaryOp_ShiftRight" => Some(types.integer)
              case "Scalar_DNumber" => Some(types.double)
              case "Scalar_String" |
                   "Scalar_Encapsed" |
                   "Expr_BinaryOp_Concat" => Some (types.string)
              case s: String if s startsWith "Expr_MagicConst" => Some(types.string)
              case "Expr_Array" => Some(types.array)
              case "Expr_BinaryOp_BooleanAnd" |
                   "Expr_BinaryOp_BooleanOr" |
                   "Expr_BinaryOp_Equal" |
                   "Expr_BinaryOp_Greater" |
                   "Expr_BinaryOp_GreaterOrEqual" |
                   "Expr_BinaryOp_Identical" |
                   "Expr_BinaryOp_LogicalAnd" |
                   "Expr_BinaryOp_LogicalOr" |
                   "Expr_BinaryOp_LogicalXor" |
                   "Expr_BinaryOp_NotEqual" |
                   "Expr_BinaryOp_NotIdentical" |
                   "Expr_BinaryOp_Smaller" |
                   "Expr_BinaryOp_SmallerOrEqual" => Some(types.boolean)
              case _ => None
            }

            possibleType foreach { node --| POSSIBLE_TYPE |--> _ }

            (dto.id, node)
        }
      }.toMap

      data.edges.foreach { dto =>
        val start = knownNodes(dto.from)
        val end = knownNodes(dto.to)

        val rel = (start --| dto.label |--> end).<

        dto.properties.foreach { case (key, value) => rel(key) = value }
      }
    }
  }

  protected def buildPrimitiveDataTypes(backend: BackendInterface) = {
    val buildType = (name: String, collection: Boolean) => {
      backend.nodes.merge(
        new Query(
          Type,
          Map("name" -> name, "primitive" -> Boolean.box(true), "collection" -> Boolean.box(collection))
        )
      )
    }

    types.integer = buildType("integer", false)
    types.double = buildType("double", false)
    types.string = buildType("string", false)
    types.array = buildType("array", true)
    types.boolean = buildType("boolean", false)
  }

}

object NodeImportService {

  class Types {
    var integer: Node = null
    var double: Node = null
    var string: Node = null
    var array: Node = null
    var boolean: Node = null
  }

  case class ImportRequest(project: String, data: ImportDataSet)

  case class WipeRequest(project: String)

  case class WipeSuccess()

  case class AllClear()

}