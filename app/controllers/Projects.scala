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

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import domain.model.Project
import domain.repository.ProjectRepository
import domain.repository.ProjectRepository._
import play.api.libs.json._
import play.api.mvc._
import controllers.helpers.ActionHelpers._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class Projects @Inject()(actorSystem: ActorSystem) extends Controller {

  val projectRepository = actorSystem.actorOf(Props[ProjectRepository], "projects")

  implicit val timeout = Timeout(1.second)

  implicit val additionalTransformationReads = (
    (JsPath \ "when").read[String] and
      (JsPath \ "cypher").read[String]
    )(Project.AdditionalTransformation.apply _)

  implicit val additionalTransformationWrites = (
    (JsPath \ "when").write[String] and
    (JsPath \ "cypher").write[String]
    )(unlift(Project.AdditionalTransformation.unapply))

  class ProjectWrites[T](implicit r: Request[T]) extends Writes[Project] {
    def writes(p: Project) = Json.obj(
      "__href" -> controllers.routes.Projects.show(p.slug).absoluteURL(),
      "__actions" -> Json.arr(
        Json.obj(
          "rel" -> "snapshot",
          "href" -> controllers.routes.Snapshots.create(p.slug).absoluteURL()
        )
      ),
      "__links" -> Json.arr(
        Json.obj(
          "rel" -> "snapshots",
          "href" -> controllers.routes.Snapshots.list(p.slug).absoluteURL()
        )
      ),
      "name" -> p.name,
      "slug" -> p.slug,
      "additionalTransformations" -> JsArray(p.additionalTransformations.toSeq.map { t =>
        Json.obj("when" -> t.when, "cypher" -> t.cypher)
      })
    )
  }

  private def buildProjectReads(slug: String): Reads[Project] = {
    (
      (JsPath \ "name").read[String] and
        (JsPath \ "additionalTransformations").read[Seq[Project.AdditionalTransformation]]
      )((a, b) => new Project(slug, a, b))
  }

//  implicit val projectReads: Reads[Project] =

//  class ProjectReads(slug: String) extends Reads[Project] {
//    override def reads(json: JsValue): JsResult[Project] = json match {
//      case o: JsObject =>
//        o \ "name" match {
//          case JsString(n: String) => JsSuccess(Project(slug, n))
//          case _ => JsError("missing-name")
//        }
//      case _ => JsError("not-an-object")
//    }
//  }

  def list = Action.async { implicit r =>
    implicit val projectWrites = new ProjectWrites()
    projectRepository ? ProjectQuery() map {
      case ProjectResponseSet(projects) => Ok(Json.toJson(projects)).withHeaders("X-ObjectCount" -> projects.size.toString)
      case anything => {
        println(anything)
        InternalServerError("Whut?!")
      }
    }
  }

  def show(slug: String) = ProjectAction(slug, projectRepository) { implicit r =>
    implicit val projectWrites = new ProjectWrites()
    Ok(Json.toJson(r.project))
  }

  def upsert(slug: String) = Action.async(BodyParsers.parse.json) { implicit r =>
    implicit val projectWrites: Writes[Project] = new ProjectWrites()
    implicit val projectReads = buildProjectReads(slug)

    r.body.validate[Project].fold(
      errors => Future {
        BadRequest(Json.obj("message" -> JsError.toFlatJson(errors)))
      },
      project => {
        projectRepository ? ProjectQuery(slug = slug, one = true) flatMap {
          case ProjectResponse(p) => projectRepository ? UpdateProject(project) map { r => Ok(Json.toJson(project)) }
          case ProjectEmptyResponse() => projectRepository ? AddProject(project) map { r => Created(Json.toJson(project)) }
          case _ => Future {
            InternalServerError(Json.obj("status" -> "ko", "message" -> "Unknown project status"))
          }
        }
      }
    )
  }

  def delete(slug: String) = Action.async { implicit r =>
    projectRepository ? DeleteProjectByQuery(ProjectQuery(slug = slug)) map {
      case true => Ok(Json.obj("status" -> "ok", "message" -> "Project deleted"))
      case false => InternalServerError(Json.obj("status" -> "ko", "message" -> "Some error"))
    }
  }

}
