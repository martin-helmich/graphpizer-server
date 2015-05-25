package controllers

import javax.inject.{Inject, Singleton}

import domain.model.ModelEdgeType._
import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import persistence.NodeWrappers._

@Singleton
class Classes @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action.async { implicit r =>
    val future = Future {
      manager connect project transactional { (b, t) =>
        JsArray(
          b execute "MATCH (c:Class) RETURN c" map { (clazz: Node) =>

            val properties = (clazz >--> HAS_PROPERTY map {
              _.end
            } map { p =>
              val types = (p >--> POSSIBLE_TYPE map {
                _.end
              } map { t =>
                Json.obj(
                  "name" -> t.property[String]("name"),
                  "__href" -> controllers.routes.Types.show(project, t ! "slug").absoluteURL(),
                  "__id" -> t.id
                )
              }).toArray

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

            Json.obj(
              "__id" -> clazz.id,
              "__href" -> controllers.routes.Classes.show(project, clazz ! "slug").absoluteURL(),
              "fqcn" -> clazz.property[String]("fqcn"),
              "namespace" -> clazz.property[String]("namespace"),
              "final" -> clazz.property[Boolean]("final"),
              "abstract" -> clazz.property[Boolean]("abstract"),
              "properties" -> properties
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
