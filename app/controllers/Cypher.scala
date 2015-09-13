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

import javax.inject.{Singleton, Inject}

import org.neo4j.graphdb._
import persistence.ConnectionManager
import play.api.libs.json._
import play.api.mvc.{BodyParsers, Action, Controller}
import views.cypher.{TexTableResultView, DotResultView, JsonTableResultView, GraphResultView}
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class Cypher @Inject()(manager: ConnectionManager) extends Controller {

  import Cypher._
  import controllers.helpers.JsonHelpers._

  def execute(project: String, format: String) = Action.async(BodyParsers.parse.json) { request =>
    Future {
      implicit val objectReads = new JsonObjectReads()
      implicit val reads = Json.reads[Query]
      request.body.validate[Query].fold(
        errors => {BadRequest(JsError.toFlatJson(errors)) },
        query => {
          try {
            val view = (query.graph.getOrElse(false), format) match {
              case (_, "dot") => new DotResultView
              case (_, "tex") => new TexTableResultView
              case (true, "json") => new GraphResultView
              case (false, "json") => new JsonTableResultView
            }

            manager connect project transactional { (b, _) =>
              val result = b execute query.cypher run()
              val columns = result.columns()

              view(result, columns) match {
                case j: JsValue => Ok(Json.toJson(j))
                case s: String => Ok(s)
                case _ => NotImplemented(Json.obj("status" -> "ko", "message" -> "Unknown view result"))
              }
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

  case class Query(cypher: String, params: Option[Map[String, AnyRef]] = None, graph: Option[Boolean] = None)

}