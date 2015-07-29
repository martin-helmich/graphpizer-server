package domain.model

import domain.model.Property.PropertyBuilder
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._
import scala.collection.JavaConversions._

abstract class ClassLike(name: String,
                         namespace: Option[String]) {

  def fqcn = (namespace map { _ + "\\" } getOrElse "") + name

  def fqcn(namespaceSeparator: String) = (namespace map {
    _.replaceAll(
      "\\\\",
      namespaceSeparator
    ) + namespaceSeparator
  } getOrElse "") + name

}

case class Class(name: String,
                 namespace: Option[String],
                 isAbstract: Boolean = false,
                 isFinal: Boolean = false,
                 properties: Seq[Property] = Seq(),
                 parent: Option[Class] = None,
                 implements: Seq[Interface] = Seq(),
                 usesTraits: Seq[Trait] = Seq()) extends ClassLike(name, namespace)

case class Interface(name: String,
                     namespace: Option[String],
                     parent: Option[Interface] = None) extends ClassLike(name, namespace)

case class Trait(name: String,
                 namespace: Option[String],
                 properties: Seq[Property] = Seq()) extends ClassLike(name, namespace)

object ClassLike {

  def fromNode(n: Node): ClassLike = if (n.hasLabel(ModelLabelType.Class)) {
    new Class(
      isAbstract = n.property[Boolean]("abstract").getOrElse(false),
      isFinal = n.property[Boolean]("final").getOrElse(false),
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      properties = (n out ModelEdgeType.HAS_PROPERTY).toSeq.map { r =>
        val propertyBuilder = new PropertyBuilder
        propertyBuilder.fromNode(r.end)
        propertyBuilder.build()
      },
      parent = (n out ModelEdgeType.EXTENDS).headOption.map { r => fromNode(r.end).asInstanceOf[Class] },
      implements = (n out ModelEdgeType.IMPLEMENTS).toSeq.map { r => fromNode(r.end).asInstanceOf[Interface] },
      usesTraits = (n out ModelEdgeType.USES_TRAIT).toSeq.map { r => fromNode(r.end).asInstanceOf[Trait] }
    )
  } else if (n.hasLabel(ModelLabelType.Interface)) {
    new Interface(
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      parent = (n out ModelEdgeType.EXTENDS).headOption.map { r => fromNode(r.end).asInstanceOf[Interface] }
    )
  } else if (n.hasLabel(ModelLabelType.Trait)) {
    new Trait(
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      properties = (n out ModelEdgeType.HAS_PROPERTY).toSeq.map { r =>
        val propertyBuilder = new PropertyBuilder
        propertyBuilder.fromNode(r.end)
        propertyBuilder.build()
      }
    )
  } else {
    throw new Exception("Bad node: " + n.getLabels.map { _.name }.mkString(", "))
  }


}