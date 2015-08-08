package domain.model

import domain.model
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._
import scala.collection.JavaConversions._

sealed trait ClassLike {
  val slug: String
  val name: String
  val namespace: Option[String]
  val methods: Seq[Method]

  def fqcn = (namespace map { _ + "\\" } getOrElse "") + name

  def fqcn(namespaceSeparator: String) = (namespace map {
    _.replaceAll(
      "\\\\",
      namespaceSeparator
    ) + namespaceSeparator
  } getOrElse "") + name

  override def equals(o: Any) = o match {
    case that: ClassLike => that.fqcn == fqcn
    case _ => false
  }

  override def hashCode = fqcn.toLowerCase.hashCode
}

class Class(val slug: String,
            val name: String,
            val namespace: Option[String],
            val isAbstract: Boolean = false,
            val isFinal: Boolean = false,
            val properties: Seq[Property] = Seq(),
            val methods: Seq[Method] = Seq(),
            val parent: Option[Class] = None,
            val implements: Seq[Interface] = Seq(),
            val usesTraits: Seq[Trait] = Seq(),
            val usages: Iterable[ClassLike]) extends ClassLike {
  def usedClasses: Iterable[ClassLike] = usages
}

class Interface(val slug: String,
                val name: String,
                val namespace: Option[String],
                val methods: Seq[Method] = Seq(),
                val parent: Option[Interface] = None) extends ClassLike

class Trait(val slug: String,
            val name: String,
            val namespace: Option[String],
            val methods: Seq[Method] = Seq(),
            val properties: Seq[Property] = Seq()) extends ClassLike

object ClassLike {

  object Visibility extends Enumeration {
    type Visibility = Value
    val Public, Protected, Private = Value
  }

  def fromNode(n: Node): ClassLike = if (n.hasLabel(ModelLabelTypes.Class)) {
    new Class(
      slug = n.property[String]("slug").getOrElse(""),
      isAbstract = n.property[Boolean]("abstract").getOrElse(false),
      isFinal = n.property[Boolean]("final").getOrElse(false),
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      properties = (n out ModelEdgeTypes.HAS_PROPERTY).toSeq.map { r => Property.fromNode(r.end) },
      methods = (n out ModelEdgeTypes.HAS_METHOD).toSeq.map { r => Method.fromNode(r.end) },
      parent = (n out ModelEdgeTypes.EXTENDS).headOption.map { r => fromNode(r.end).asInstanceOf[Class] },
      implements = (n out ModelEdgeTypes.IMPLEMENTS).toSeq.map { r => fromNode(r.end).asInstanceOf[Interface] },
      usesTraits = (n out ModelEdgeTypes.USES_TRAIT).toSeq.map { r => fromNode(r.end).asInstanceOf[Trait] },
      usages = Seq()
    ) {
      override def usedClasses: Iterable[ClassLike] = {
        (n out ModelEdgeTypes.USES).flatMap { r => r.end.out(ModelEdgeTypes.IS).map { _.end } }.map { c => fromNode(c)}
      }
    }
  } else if (n.hasLabel(ModelLabelTypes.Interface)) {
    new Interface(
      slug = n.property[String]("slug").getOrElse(""),
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      methods = (n out ModelEdgeTypes.HAS_METHOD).toSeq.map { r => Method.fromNode(r.end) },
      parent = (n out ModelEdgeTypes.EXTENDS).headOption.map { r => fromNode(r.end).asInstanceOf[Interface] }
    )
  } else if (n.hasLabel(ModelLabelTypes.Trait)) {
    new Trait(
      slug = n.property[String]("slug").getOrElse(""),
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      properties = (n out ModelEdgeTypes.HAS_PROPERTY).toSeq.map { r => Property.fromNode(r.end) },
      methods = (n out ModelEdgeTypes.HAS_METHOD).toSeq.map { r => Method.fromNode(r.end) }
    )
  } else {
    throw new Exception("Bad node: " + n.getLabels.map { _.name }.mkString(", "))
  }


}