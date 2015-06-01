package controllers

import javax.inject.Inject

import domain.model.Snapshot
import domain.repository.ProjectRepository
import persistence.ConnectionManager
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Snapshots @Inject()(projectRepository: ProjectRepository, connectionManager: ConnectionManager) extends Controller {

  implicit val snapshotWrites = new Writes[Snapshot] {
    def writes(o: Snapshot): JsValue = {
      Json.obj("id" -> o.id.toString)
    }
  }

  def list(slug: String) = Action.async {
    projectRepository findBySlug slug map {
      case Some(project) => Ok(Json.toJson(project.snapshots))
      case _ => NotFound("Gubbel!")
    }
  }

  def create(slug: String) = Action { implicit r =>
    projectRepository findBySlug slug foreach { case Some(p) =>
      p.snapshots += connectionManager.snapshot(p.slug)
      projectRepository.update(p)
    }
    Ok("foo")
  }

}
