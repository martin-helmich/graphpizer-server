package domain.model

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

import domain.model
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._
import scala.collection.JavaConversions._

sealed trait ClassLike {
  val slug: String
  val name: String
  val namespace: Option[String]
  val methods: Seq[Method]
//  val packageName: Option[String]

  def packageName: Option[String] = None

  def fqcn = (namespace map {
    _ + "\\"
  } getOrElse "") + name

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
            val usages: Iterable[ClassLike] = Seq()) extends ClassLike {
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
        (n out ModelEdgeTypes.USES).flatMap { r => r.end.out(ModelEdgeTypes.IS).map {
          _.end
        }
        }.map { c => fromNode(c) }
      }

      override def packageName: Option[String] = {
        (n out ModelEdgeTypes.MEMBER_OF_PACKAGE).headOption.flatMap { r =>
          r.end.property[String]("name")
        }
      }
    }
  } else if (n.hasLabel(ModelLabelTypes.Interface)) {
    new Interface(
      slug = n.property[String]("slug").getOrElse(""),
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      methods = (n out ModelEdgeTypes.HAS_METHOD).toSeq.map { r => Method.fromNode(r.end) },
      parent = (n out ModelEdgeTypes.EXTENDS).headOption.map { r => fromNode(r.end).asInstanceOf[Interface] }
    ) {
      override def packageName: Option[String] = {
        (n out ModelEdgeTypes.MEMBER_OF_PACKAGE).headOption.flatMap { r =>
          r.end.property[String]("name")
        }
      }
    }
  } else if (n.hasLabel(ModelLabelTypes.Trait)) {
    new Trait(
      slug = n.property[String]("slug").getOrElse(""),
      name = n.property[String]("name").get,
      namespace = n.property[String]("namespace"),
      properties = (n out ModelEdgeTypes.HAS_PROPERTY).toSeq.map { r => Property.fromNode(r.end) },
      methods = (n out ModelEdgeTypes.HAS_METHOD).toSeq.map { r => Method.fromNode(r.end) }
    ) {
      override def packageName: Option[String] = {
        (n out ModelEdgeTypes.MEMBER_OF_PACKAGE).headOption.flatMap { r =>
          r.end.property[String]("name")
        }
      }
    }
  } else {
    throw new Exception("Bad node: " + n.getLabels.map {
      _.name
    }.mkString(", "))
  }


}