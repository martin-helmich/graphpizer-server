package controllers

import javax.inject.{Singleton, Inject}

import org.neo4j.graphdb.{Relationship, Node, QueryExecutionException}
import persistence.ConnectionManager
import play.api.libs.json._
import play.api.mvc.{BodyParsers, Action, Controller}
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import persistence.NodeWrappers._

@Singleton
class Cypher @Inject()(manager: ConnectionManager) extends Controller {

  import Cypher._
  import controllers.helpers.JsonHelpers._

  def execute(project: String) = Action.async(BodyParsers.parse.json) { request =>
    Future {
      implicit val objectReads = new JsonObjectReads()
      implicit val reads = Json.reads[Query]
      request.body.validate[Query].fold(
        errors => {BadRequest(JsError.toFlatJson(errors)) },
        query => {
          try {
            manager connect project transactional { (b, _) =>
              val result = b execute query.cypher run()
              val columns = result.columns()

              lazy val anyRefToJsValue: (Any) => JsValue = (a: Any) => a match {
                case a: Boolean => JsBoolean(a)
                case a: Double => JsNumber(a)
                case a: Int => JsNumber(a)
                case a: String => JsString(a)
                case a: Map[String, AnyRef] => JsObject(a.toSeq.map { case (k, v) => (k, anyRefToJsValue(v)) })
                case _ => JsNull
              }

              val json = result.map { r =>
                JsObject(
                  columns map { name =>
                    (name, r get name match {
                      case n: Node => Json.obj(
                        "id" -> n.id,
                        "labels" -> n.getLabels.map { _.name },
                        "properties" -> JsObject(n.getPropertyKeys.map { p => (p, anyRefToJsValue(n.getProperty(p))) }.toSeq)
                      )
                      case r: Relationship => Json.obj(
                        "id" -> r.id,
                        "type" -> r.getType.name,
                        "properties" -> JsObject(r.getPropertyKeys.map { p => (p, anyRefToJsValue(r.getProperty(p))) }.toSeq)
                      )
                      case null => JsNull
                      case blub => println(blub); Json.obj("whut" -> "??")
                    })
                  }
                )
              }.toSeq

              Ok(Json.toJson(json))
            }
          } catch {
            case e: QueryExecutionException => BadRequest(Json.obj("status" -> "ko", "message" -> e.getMessage))
          }
        }
      )
    }
  }

}

object Cypher {

  case class Query(cypher: String, params: Map[String, AnyRef] = Map())

}