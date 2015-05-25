package domain.model

import persistence.Query

case class DataType(name: String, primitive: Boolean, collection: Boolean = false, inner: DataType = null) {

  def slug = {
    name.toLowerCase.replace("\\", "_").replace("<", "-").replace(">", "")
  }

  def query = {
    new Query(
      ModelLabelType.Type,
      Map(
        "name" -> name,
        "slug" -> slug,
        "primitive" -> Boolean.box(primitive),
        "collection" -> Boolean.box(collection)
      )
    )
  }

}
