package domain.model

import org.neo4j.graphdb.Node
import persistence.Query
import persistence.NodeWrappers._

case class DataType(name: String, primitive: Boolean, collection: Boolean = false, inner: Option[DataType] = None) {

  def classLike: Option[ClassLike] = None

  def slug = {
    name.toLowerCase.replace("\\", "-").replace("<", "--").replace(">", "")
  }

  def query = new Query(
    ModelLabelTypes.Type,
    Map(
      "name" -> name,
      "primitive" -> Boolean.box(primitive),
      "collection" -> Boolean.box(collection)
    )
  )

}

object DataType {

  def fromNode(n: Node): DataType = new DataType(
    n.property[String]("name").getOrElse(""),
    n.property[Boolean]("primitive").getOrElse(false),
    n.property[Boolean]("collection").getOrElse(false),
    (n out ModelEdgeTypes.COLLECTION_OF).headOption.map { r => fromNode(r.end) }
  ) {
    override def classLike: Option[ClassLike] =
      (n out ModelEdgeTypes.IS).headOption.map { r => ClassLike.fromNode(r.end) }
  }

}