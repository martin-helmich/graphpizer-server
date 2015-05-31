package controllers

import javax.inject.{Inject, Singleton}

import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import persistence.NodeWrappers._

import scala.collection.JavaConversions._
import scala.concurrent.Future

@Singleton
class Packages @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action.async {
    val future = Future {
      manager connect project transactional { (b, _) =>
        val f = b execute "MATCH (p:Package)-[:CONTAINS_FILE]->(f) RETURN p, COUNT(f)" map { (pkg: Node, fileCount: Long) =>
          Json.obj(
            "name" -> pkg[String]("name").get,
            "fileCount" -> fileCount
          )
        }
        JsArray(f)

        //        val nodes: Iterator[Node] = result.columnAs[Node]("p")
        //
        //          nodes.toArray.map({ n =>
        //            Json.obj(
        //              "name" -> (n getProperty "name").asInstanceOf[String]
        //            )
        //          })
        //        )
      }
    }
    future map { json => Ok(json) }
  }

}
