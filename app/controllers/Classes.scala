package controllers

import java.io.{ByteArrayOutputStream, BufferedOutputStream, OutputStream}
import javax.inject.{Inject, Singleton}

import controllers.helpers.ViewHelpers
import domain.model.ModelEdgeTypes._
import domain.model.ModelLabelTypes
import net.sourceforge.plantuml.SourceStringReader
import org.neo4j.graphdb.{Direction, Node}
import persistence.ConnectionManager
import play.api.{Logger, Play}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Future
import persistence.NodeWrappers._

@Singleton
class Classes @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action.async { implicit req =>
    val future = Future {
      manager connect project transactional { (b, t) =>
        JsArray(
          b execute "MATCH (c:Class) RETURN c" map { (clazz: Node) =>
            try {
              val properties = (clazz >--> HAS_PROPERTY map { _.end } map { p =>
                val types = (p >--> POSSIBLE_TYPE map { r => ViewHelpers.writeTypeRef(project, r.end) }).toArray

                Json.obj(
                  "name" -> p.property[String]("name"),
                  "public" -> p.property[Boolean]("public"),
                  "protected" -> p.property[Boolean]("protected"),
                  "private" -> p.property[Boolean]("private"),
                  "static" -> p.property[Boolean]("static"),
                  "docComment" -> p.property[String]("docComment"),
                  "possibleTypes" -> types,
                  "__id" -> p.id
                )
              }).toArray

              val methods = (clazz >--> HAS_METHOD map { _.end } map { m =>
                val types = (m >--> POSSIBLE_TYPE map { r => ViewHelpers.writeTypeRef(project, r.end) }).toArray
                val params = (m >--> HAS_PARAMETER map { _.end } map { p =>
                  val types = (p >--> POSSIBLE_TYPE map { r => ViewHelpers.writeTypeRef(project, r.end) }).toArray
                  Json.obj(
                    "name" -> p.property[String]("name"),
                    "variadic" -> p.property[Boolean]("variadic"),
                    "byRef" -> p.property[Boolean]("byRef"),
                    "possibleTypes" -> types
                  )
                }).toArray

                Json.obj(
                  "name" -> m.property[String]("name"),
                  "public" -> m.property[Boolean]("public"),
                  "protected" -> m.property[Boolean]("protected"),
                  "private" -> m.property[Boolean]("private"),
                  "static" -> m.property[Boolean]("static"),
                  "abstract" -> m.property[Boolean]("abstract"),
                  "docComment" -> m.property[String]("docComment"),
                  "possibleReturnTypes" -> types,
                  "parameters" -> params
                )
              }).toList

              val parents = (clazz >--> EXTENDS map { r => ViewHelpers.writeClassRef(project, r.end) }).toArray
              val implements = (clazz >--> IMPLEMENTS map { r => ViewHelpers.writeInterfaceRef(
                project,
                r.end
              )
              }).toArray

              Json.obj(
                "__id" -> clazz.id,
                "__href" -> controllers.routes.Classes.show(project, clazz ! "slug").absoluteURL(),
                "fqcn" -> clazz.property[String]("fqcn"),
                "namespace" -> clazz.property[String]("namespace"),
                "final" -> clazz.property[Boolean]("final"),
                "abstract" -> clazz.property[Boolean]("abstract"),
                "properties" -> properties,
                "methods" -> methods,
                "extends" -> (if (parents.nonEmpty) parents.head else JsNull),
                "implements" -> implements
              )
            } catch {
              case e: Exception =>
                Logger.warn(e.getMessage, e)
                Json.obj(
                  "__id" -> clazz.id,
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
      val classes = b execute cypher map { (classLike: Node, pkg: String) =>
        domain.model.ClassLike.fromNode(classLike)
      }

      val umlcode = views.plantuml.ClassDiagram(classes)

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
