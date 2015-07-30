package domain.model

import domain.model.ClassLike.Visibility
import domain.model.ClassLike.Visibility.Visibility
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

case class Method(name: String,
                  visibility: Visibility,
                  isStatic: Boolean = false,
                  isAbstract: Boolean = false,
                  possibleReturnTypes: Seq[DataType] = Seq(),
                  parameters: Seq[Parameter] = Seq()) {

}


case class Parameter(name: String,
                     optional: Boolean = false,
                     isVariadic: Boolean = false,
                     isByReference: Boolean = false,
                     possibleTypes: Seq[DataType] = Seq())

object Method {

  def fromNode(n: Node): Method = new Method(
    name = n.property[String]("name").getOrElse(""),
    isStatic = n.property[Boolean]("static").getOrElse(false),
    isAbstract = n.property("abstract").getOrElse(false),
    visibility = if (n.property[Boolean]("private").getOrElse(false)) {
      Visibility.Private
    } else if (n.property[Boolean]("protected").getOrElse(false)) {
      Visibility.Protected
    } else {
      Visibility.Public
    },
    parameters = (n out ModelEdgeTypes.HAS_PARAMETER).toSeq.map { r => Parameter.fromNode(r.end) },
    possibleReturnTypes = (n out ModelEdgeTypes.POSSIBLE_TYPE).toSeq.map { r => DataType.fromNode(r.end) }
  )

}

object Parameter {
  def fromNode(n: Node): Parameter = new Parameter(
    name = n.property("name").getOrElse(""),
    optional = n.property("hasDefaultValue").getOrElse(false),
    isVariadic = n.property("variadic").getOrElse(false),
    isByReference = n.property("byRef").getOrElse(false),
    possibleTypes = (n out ModelEdgeTypes.POSSIBLE_TYPE).toSeq.map { r => DataType.fromNode(r.end) }
  )
}