package controllers

import javax.inject.{Inject, Singleton}

import controllers.helpers.ViewHelpers
import domain.model.ModelEdgeType._
import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import persistence.NodeWrappers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, JsNull, Json}
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class Interfaces @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action { implicit req =>
    val json = manager connect project transactional { (b, t) =>
      JsArray(
        b execute "MATCH (c:Interface) RETURN c" map { (iface: Node) =>

          val methods = (iface >--> HAS_METHOD map { _.end } map { m =>
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
              "docComment" -> m.property[String]("docComment"),
              "possibleReturnTypes" -> types,
              "parameters" -> params
            )
          }).toList

          val parents = (iface >--> EXTENDS map { r => ViewHelpers.writeClassRef(project, r.end) }).toArray

          Json.obj(
            "__id" -> iface.id,
            "__href" -> controllers.routes.Interfaces.show(project, iface ! "slug").absoluteURL(),
            "fqcn" -> iface.property[String]("fqcn"),
            "namespace" -> iface.property[String]("namespace"),
            "methods" -> methods,
            "extends" -> parents
          )
        }
      )
    }
    Ok(json)
  }

  def show(project: String, name: String) = Action {
    Ok("Huhu!")
  }

}
