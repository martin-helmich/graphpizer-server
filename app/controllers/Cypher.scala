package controllers

import javax.inject.{Singleton, Inject}

import persistence.ConnectionManager
import play.api.libs.json._
import play.api.mvc.{BodyParsers, Action, Controller}
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class Cypher @Inject()(manager: ConnectionManager) extends Controller {
  import Cypher._
  import controllers.helpers.JsonHelpers._

  def execute(project: String) = Action.async(BodyParsers.parse.json) { request =>
    Future {
      implicit val objectReads = new JsonObjectReads()
      implicit val reads = Json.reads[Query]
      request.body.validate[Query].fold(
        errors => { BadRequest(JsError.toFlatJson(errors)) },
        query => {
          manager connect project transactional { (b, _) =>
            val result = b execute query.cypher run()
            val columns = result.columns()

            val json = result.map { r =>
              JsObject(
                columns map { name =>
                  (name, JsString("foo"))
                }
              )
            }.toSeq

            Ok(Json.toJson(json))
          }
        }
      )
    }
  }

}

object Cypher {
  case class Query(cypher: String, params: Map[String, AnyRef] = Map())
}