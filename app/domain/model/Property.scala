package domain.model

import domain.model.Property.Visibility.Visibility
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

case class Property(name: String,
                    visibility: Visibility,
                    static: Boolean = false,
                    possibleTypes: Seq[DataType] = Seq(),
                    docComment: String) {

}

object Property {

  object Visibility extends Enumeration {
    type Visibility = Value
    val Public, Protected, Private = Value
  }

  class PropertyBuilder {
    var name: String = ""
    var visibility: Visibility = Visibility.Public
    var static: Boolean = false
    var possibleTypes: Seq[DataType] = Seq()
    var docComment: String = ""

    def fromNode(n: Node): Unit = {
      name = n.property[String]("name").getOrElse("")
      static = n.property[Boolean]("static").getOrElse(false)
      docComment = n.property[String]("docComment").getOrElse("")
      possibleTypes = (n out ModelEdgeType.POSSIBLE_TYPE).toSeq.map { r => DataType.fromNode(r.end) }

      if (n.property[Boolean]("protected").getOrElse(false)) {
        visibility = Visibility.Protected
      }
      if (n.property[Boolean]("private").getOrElse(false)) {
        visibility = Visibility.Private
      }

    }

    def build(): Property = new Property(
      name, visibility, static, possibleTypes, docComment
    )
  }

}