package controllers

import javax.inject.{Inject, Singleton}

import controllers.helpers.ViewHelpers
import domain.model.ModelEdgeType._
import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsNull, JsArray, Json}
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import persistence.NodeWrappers._

@Singleton
class Classes @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action.async { implicit req =>
    val future = Future {
      manager connect project transactional { (b, t) =>
        JsArray(
          b execute "MATCH (c:Class) RETURN c" map { (clazz: Node) =>

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
            val implements = (clazz >--> IMPLEMENTS map { r => ViewHelpers.writeInterfaceRef(project, r.end) }).toArray

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
          }
        )
      }
    }
    future map { json => Ok(json) }
  }

  def show(project: String, name: String) = Action {
    Ok("Huhu!")
  }

}
