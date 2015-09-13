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

import javax.inject.{Inject, Singleton}

import akka.actor.{Props, ActorSystem}
import domain.repository.ProjectRepository
import org.neo4j.graphdb.Node
import persistence.{Query, ConnectionManager}
import persistence.NodeWrappers._
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import domain.model.AstLabelType._
import controllers.helpers.ActionHelpers._

@Singleton
class Files @Inject()(manager: ConnectionManager, actorSystem: ActorSystem) extends Controller {

  val projects = actorSystem.actorOf(Props[ProjectRepository], name = "projects")

  val FileNotFound = (p: String) => NotFound(Json.obj("status" -> "ko", "messsage" -> s"File '$p' does not exist"))

  def list(project: String) = ProjectAction(project, projects) { r =>
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

  def show(project: String, path: String) = ProjectAction(project, projects) { request =>
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

  def check(project: String, path: String) = ProjectAction(project, projects) { request =>
    val checksum = request.headers.get("etag")

    manager connect project transactional { (b, _) =>
      val q = new Query(File, Map("filename" -> path))
      b.nodes find q match {
        case Some(file) =>
          if (file.property[String]("checksum") == checksum) {
            NotModified
          } else {
            Ok
          }
        case None => FileNotFound(path)
      }
    }
  }

}
