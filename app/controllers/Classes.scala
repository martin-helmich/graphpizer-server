package controllers

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

import java.io.{ByteArrayOutputStream, BufferedOutputStream, OutputStream}
import javax.inject.{Inject, Singleton}

import controllers.helpers.ViewHelpers
import domain.model.ModelEdgeTypes._
import domain.model._
import domain.model.ClassLike.Visibility
import domain.model.ClassLike.Visibility.Visibility
import net.sourceforge.plantuml.SourceStringReader
import org.neo4j.graphdb.{Direction, Node}
import persistence.ConnectionManager
import play.api.{Logger, Play}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import views.plantuml.ClassDiagram

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Future
import persistence.NodeWrappers._

@Singleton
class Classes @Inject()(manager: ConnectionManager) extends Controller {

  private def propertyToJson(project: String, p: Property)(implicit request: Request[AnyContent]): JsValue = Json.obj(
    "name" -> p.name,
    "public" -> JsBoolean(p.visibility match { case Visibility.Public => true; case _ => false }),
    "protected" -> JsBoolean(p.visibility match { case Visibility.Protected => true; case _ => false }),
    "private" -> JsBoolean(p.visibility match { case Visibility.Private => true; case _ => false }),
    "static" -> p.static,
    "possibleTypes" -> p.possibleTypes.toArray.map { ViewHelpers.writeTypeRef(project, _) }
  )

  private def methodToJson(project: String, meth: Method)(implicit request: Request[AnyContent]): JsValue = Json.obj(
    "name" -> meth.name,
    "public" -> JsBoolean(meth.visibility match { case Visibility.Public => true; case _ => false }),
    "protected" -> JsBoolean(meth.visibility match { case Visibility.Protected => true; case _ => false }),
    "private" -> JsBoolean(meth.visibility match { case Visibility.Private => true; case _ => false }),
    "static" -> meth.isAbstract,
    "abstract" -> meth.isStatic,
    "parameters" -> meth.parameters.toArray.map { param => Json.obj(
      "name" -> param.name,
      "variadic" -> param.isVariadic,
      "byRef" -> param.isByReference,
      "possibleTypes" -> param.possibleTypes.toArray.map { ViewHelpers.writeTypeRef(project, _) }
    )
    },
    "possibleReturnTypes" -> meth.possibleReturnTypes.toArray.map { ViewHelpers.writeTypeRef(project, _) }
  )

  def list(project: String) = Action.async { implicit req =>
    val future = Future {
      manager connect project transactional { (b, t) =>
        JsArray(
          b execute "MATCH (c:Class) RETURN c" map { (clazz: Node) => (clazz.id, ClassLike.fromNode(clazz)) } map { case (id, clazz) =>
            try {
              clazz match {
                case c: Class => Json.obj(
                  "__id" -> id,
                  "__href" -> controllers.routes.Classes.show(project, c.slug).absoluteURL(),
                  "type" -> "class",
                  "fqcn" -> c.fqcn,
                  "name" -> c.name,
                  "namespace" -> c.namespace,
                  "final" -> c.isFinal,
                  "abstract" -> c.isAbstract,
                  "properties" -> c.properties.toArray.map { propertyToJson(project, _) },
                  "methods" -> c.methods.toArray.map { methodToJson(project, _) },
                  "extends" -> c.parent.map { ViewHelpers.writeClassRef(project, _) },
                  "implements" -> c.implements.toArray.map { ViewHelpers.writeClassRef(project, _) }
                )
                case i: Interface => Json.obj(
                  "__id" -> id,
                  "__href" -> controllers.routes.Classes.show(project, i.slug).absoluteURL(),
                  "type" -> "interface",
                  "fqcn" -> i.fqcn,
                  "name" -> i.name,
                  "namespace" -> i.namespace,
                  "abstract" -> true,
                  "methods" -> i.methods.toArray.map { methodToJson(project, _) },
                  "extends" -> i.parent.map { ViewHelpers.writeClassRef(project, _) }
                )
                case t: Trait => Json.obj(
                  "__id" -> id,
                  "__href" -> controllers.routes.Classes.show(project, t.slug).absoluteURL(),
                  "type" -> "trait",
                  "fqcn" -> t.fqcn,
                  "name" -> t.name,
                  "namespace" -> t.namespace,
                  "abstract" -> true,
                  "methods" -> t.methods.toArray.map { methodToJson(project, _) },
                  "properties" -> t.properties.toArray.map { propertyToJson(project, _) }
                )
              }
            } catch {
              case e: Exception =>
                Logger.warn(e.getMessage, e)
                Json.obj(
                  "__id" -> id,
                  "__invalidObject" -> e.getMessage
                )
            }
          }
        )
      }
    }
    future map { json => Ok(json) }
  }

  def uml(project: String, slug: String, format: String = "txt") = Action {
    manager connect project transactional { (b, _) =>
      val cypher = """MATCH (c)-[:DEFINED_IN]->()<-[:HAS|SUB*]-(:File)<-[:CONTAINS_FILE]-(p:Package) WHERE c:Class OR c:Trait OR c:Interface RETURN c, p.name"""
      val classes = b execute cypher map { (classLike: Node, pkg: String) => ClassLike.fromNode(classLike) }

      val umlconfig = ClassDiagram.DisplayConfiguration(withPackages = true, withUsages = true)
      val umlcode = ClassDiagram(classes, umlconfig)

      format match {
        case "txt" => Ok(umlcode)
        case "png" =>
          val reader = new SourceStringReader(umlcode)
          val out = new ByteArrayOutputStream(16 * 1024)

          reader.generateImage(out)
          Ok(out.toByteArray).as("image/png")
        case _ => NotAcceptable(s"Format not acceptable: $format")
      }
    }
  }

  def graph(project: String) = Action {
    val nodes = mutable.Buffer[JsValue]()
    val edges = mutable.Buffer[JsValue]()

    manager connect project transactional { (b, _) =>
      val cypher = """MATCH (c)-[:DEFINED_IN]->()<-[:HAS|SUB*]-(:File)<-[:CONTAINS_FILE]-(p:Package) WHERE c:Class OR c:Trait OR c:Interface RETURN c, p.name"""
      b execute cypher foreach { (classLike: Node, pkg: String) =>
        nodes += Json.obj(
          "id" -> classLike.id,
          "package" -> pkg,
          "type" -> classLike.getLabels.headOption.map { _.name }.orNull,
          "fqcn" -> classLike.property[String]("fqcn").orNull
        )

        classLike.getRelationships(Direction.OUTGOING).filter { r =>
          r.end ? ModelLabelTypes.Class || r.end ? ModelLabelTypes.Interface || r.end ? ModelLabelTypes.Trait
        } foreach { o =>
          edges += Json.obj(
            "from" -> classLike.id,
            "to" -> o.end.id,
            "label" -> o.getType.name
          )
        }

        classLike >--> USES map { _.end } flatMap { t => t >--> IS } map { _.end } foreach { other =>
          edges += Json.obj(
            "from" -> classLike.id,
            "to" -> other.id,
            "label" -> "USES"
          )
        }
      }
    }
    Ok(Json.toJson(Json.obj("nodes" -> nodes, "edges" -> edges)))
  }

  def show(project: String, name: String) = Action {
    Ok("Huhu!")
  }

}
