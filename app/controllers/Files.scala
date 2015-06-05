package controllers

import javax.inject.{Inject, Singleton}

import org.neo4j.graphdb.Node
import persistence.{Query, ConnectionManager}
import persistence.NodeWrappers._
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import domain.model.AstLabelType._

@Singleton
class Files @Inject()(manager: ConnectionManager) extends Controller {

  val FileNotFound = (p: String) => NotFound(Json.obj("status" -> "ko", "messsage" -> s"File '$p' does not exist"))

  def list(project: String) = Action {
    val json = manager connect project transactional { (b, t) =>
      JsArray(
        b execute "MATCH (p:File) RETURN p" map { (file: Node) =>
          Json.obj(
            "filename" -> file.property[String]("filename"),
            "checksum" -> file.property[String]("checksum")
          )
        }
      )
    }
    Ok(json)
  }

  def show(project: String, path: String) = Action { request =>
    manager connect project transactional { (b, _) =>
      val q = new Query(File, Map("filename" -> path))
      b.nodes find q match {
        case Some(file) =>
          val json = Json.obj(
            "filename" -> file.property[String]("filename"),
            "checksum" -> file.property[String]("checksum")
          )
          Ok(json)
        case None => FileNotFound(path)
      }
    }
  }

  def check(project: String, path: String) = Action { request =>
    val checksum = request.headers.get("etag")

    manager connect project transactional { (b, _) =>
      val q = new Query(File, Map("filename" -> path))
      b.nodes find q match {
        case Some(file) =>
          if (file.property[String]("checksum") == checksum) {NotModified }
          else {Ok }
        case None => FileNotFound(path)
      }
    }
  }

}
