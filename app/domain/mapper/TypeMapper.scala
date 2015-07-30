package domain.mapper

import domain.model.ModelEdgeTypes._
import domain.model.DataType
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

import scala.collection.mutable

class TypeMapper {

  protected val knownMappings = mutable.Map[Node, DataType]()

  def mapNodeToType(n: Node): DataType = {
    knownMappings get n match {
      case Some(d: DataType) => d
      case _ =>
        val t = buildFromNode(n)
        knownMappings += n -> t
        t
    }
  }

  protected def buildFromNode(n: Node): DataType = {
    val inner = n >--> COLLECTION_OF map { _.end }
    val innerType = inner.size match {
      case 0 => None
      case _ => Some(buildFromNode(inner.head))
    }

    DataType(
      n.property[String]("name").get,
      n.property[Boolean]("primitive").getOrElse(true),
      n.property[Boolean]("collection").getOrElse(false),
      innerType
    )
  }

}
