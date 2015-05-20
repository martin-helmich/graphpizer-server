package controllers

import javax.inject.{Inject, Singleton}

import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.Future

@Singleton
class Package @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action.async {
    val future = Future {
      manager connect project transactional { (b, t) =>
        val result = b execute "MATCH (p:Package) RETURN p" run
        val nodes: Iterator[Node] = result.columnAs[Node]("p")

        JsArray(
          nodes.toArray.map({ n =>
            Json.obj(
              "name" -> (n getProperty "name").asInstanceOf[String]
            )
          })
        )
      }
    }
    future map { json => Ok(json) }
  }

}
