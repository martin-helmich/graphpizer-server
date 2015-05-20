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
class File @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action {
    val json = manager connect project transactional { (b, t) =>
      val result = b execute "MATCH (p:File) RETURN p" run
      val nodes: Iterator[Node] = result.columnAs[Node]("p")

      JsArray(
        nodes.toArray.map({ n =>
          Json.obj(
            "filename" -> (n getProperty "filename").asInstanceOf[String]
          )
        })
      )
      //        JsArray(
      //          (nodes toSeq map { n => Json.obj(
      //            "filename" -> (n getProperty "filename").asInstanceOf[String]
      //          )
      //          })
      //        )
    }
    Ok(json)
  }

}
