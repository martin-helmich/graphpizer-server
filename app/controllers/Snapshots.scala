package controllers

import java.util.UUID
import javax.inject.Inject

import domain.model.{Project, Snapshot}
import domain.repository.ProjectRepository
import persistence.ConnectionManager
import play.api.mvc.{AnyContent, Request, Action, Controller}
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Snapshots @Inject()(projectRepository: ProjectRepository, connectionManager: ConnectionManager) extends Controller {

  class SnapshotWriter(p: Project)(implicit r: Request[AnyContent]) extends Writes[Snapshot] {
    def writes(o: Snapshot): JsValue = {
      Json.obj(
        "__actions" -> Json.arr(
          Json.obj(
            "rel" -> "restore",
            "href" -> controllers.routes.Snapshots.restore("default", o.id).absoluteURL()
          )
        ),
        "id" -> o.id.toString,
        "timestamp" -> o.timestamp.toString,
        "size" -> o.size
      )
    }
  }

  def list(slug: String) = Action.async { implicit r =>
    projectRepository findBySlug slug map {
      case Some(project) =>
        implicit val w = new SnapshotWriter(project)
        Ok(Json.toJson(project.snapshots))
      case _ => NotFound("Gubbel!")
    }
  }

  def create(slug: String) = Action { implicit r =>
    projectRepository findBySlug slug foreach { case Some(p) =>
      p.snapshots += connectionManager.snapshot(p.slug)
      projectRepository.update(p)
    }
    Accepted(Json.obj("status" -> "ok", "message" -> "Snapshot started"))
  }

  def restore(slug: String, id: UUID) = Action { implicit r =>
    Ok("foo")
  }

}
