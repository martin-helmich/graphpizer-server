package controllers

import org.neo4j.graphdb.Node
import persistence.{ConnectionManager, Backend}
import play.api._
import play.api.libs.json.{JsValue, JsArray, Json}
import play.api.mvc._
import collection.JavaConversions._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Package extends Controller {

  val connManager = ConnectionManager

  def list(project: String) = Action.async {
    val future = Future {
      connManager connect project transactional { (b, t) =>
        val result = b.execute("MATCH (p:File) RETURN p")
        val nodes: Iterator[Node] = result.columnAs[Node]("p")

        JsArray(
          (nodes map { n => Json.obj(
            "filename" -> n.getProperty("filename").asInstanceOf[String],
            "checksum" -> n.getProperty("checksum", null).asInstanceOf[String]
          )
          }).toSeq
        )
      }
    }
    future map { json => Ok(json) }
  }

}
