package domain.model

import persistence.Query

case class DataType(name: String, primitive: Boolean, collection: Boolean = false, inner: Option[DataType] = None) {

  def slug = {
    name.toLowerCase.replace("\\", "-").replace("<", "--").replace(">", "")
  }

  def query = new Query(
    ModelLabelType.Type,
    Map(
      "name" -> name,
      "primitive" -> Boolean.box(primitive),
      "collection" -> Boolean.box(collection)
    )
  )

}
