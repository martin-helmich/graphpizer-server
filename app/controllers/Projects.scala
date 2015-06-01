package controllers

import javax.inject.{Inject, Singleton}

import domain.model.Project
import domain.repository.ProjectRepository
import domain.repository.ProjectRepository._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Singleton
class Projects @Inject()(projectRepository: ProjectRepository) extends Controller {

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
      "slug" -> p.slug
    )
  }

  class ProjectReads(slug: String) extends Reads[Project] {
    override def reads(json: JsValue): JsResult[Project] = json match {
      case o: JsObject =>
        o \ "name" match {
          case JsString(n: String) => JsSuccess(Project(slug, n))
          case _ => JsError("missing-name")
        }
      case _ => JsError("not-an-object")
    }
  }

  def list = Action.async { implicit r =>
    implicit val projectWrites = new ProjectWrites()
    projectRepository ? ProjectQuery() map {
      case ProjectResponseSet(projects) => Ok(Json.toJson(projects)).withHeaders("X-ObjectCount" -> projects.size.toString)
      case _ => InternalServerError("Whut?!")
    }
  }

  def show(slug: String) = Action.async { implicit r =>
    implicit val projectWrites = new ProjectWrites()
    projectRepository ? ProjectQuery(slug = slug, one = true) map {
      case ProjectResponse(p) => Ok(Json.toJson(p))
      case ProjectEmptyResponse() => NotFound(Json.obj("status" -> "notfound", "message" -> s"Project $slug does not exist"))
    }
  }

  def upsert(slug: String) = Action.async(BodyParsers.parse.json) { implicit r =>
    implicit val projectWrites: Writes[Project] = new ProjectWrites()
    implicit val projectReads = new ProjectReads(slug)

    r.body.validate[Project].fold(
      errors => Future { BadRequest(Json.obj("message" -> JsError.toFlatJson(errors))) },
      project => {
        projectRepository ? ProjectQuery(slug = slug, one = true) flatMap {
          case ProjectResponse(p) => projectRepository ! UpdateProject(project) map { r => Ok(Json.toJson(project)) }
          case ProjectEmptyResponse() => projectRepository ! AddProject(project) map { r => Created(Json.toJson(project)) }
          case _ => Future { InternalServerError(Json.obj("status" -> "ko", "message" -> "Unknown project status")) }
        }
      }
    )
  }

  def delete(slug: String) = Action.async { implicit r =>
    projectRepository ! DeleteProjectByQuery(ProjectQuery(slug = slug)) map {
      case true => Ok(Json.obj("status" -> "ok", "message" -> "Project deleted"))
      case false => InternalServerError(Json.obj("status" -> "ko", "message" -> "Some error"))
    }
  }

}
