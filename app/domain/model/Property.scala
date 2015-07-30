package domain.model

import domain.model.ClassLike.Visibility.Visibility
import domain.model.ClassLike.Visibility
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

case class Property(name: String,
                    visibility: Visibility,
                    static: Boolean = false,
                    possibleTypes: Seq[DataType] = Seq(),
                    docComment: String) {

}

object Property {

  def fromNode(n: Node): Property = new Property(
    name = n.property[String]("name").getOrElse(""),
    static = n.property[Boolean]("static").getOrElse(false),
    docComment = n.property[String]("docComment").getOrElse(""),
    possibleTypes = (n out ModelEdgeTypes.POSSIBLE_TYPE).toSeq.map { r => DataType.fromNode(r.end) },
    visibility = if (n.property[Boolean]("protected").getOrElse(false)) {
      Visibility.Protected
    } else if (n.property[Boolean]("private").getOrElse(false)) {
      Visibility.Private
    } else {
      Visibility.Public
    }
  )

}