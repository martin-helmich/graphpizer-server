package controllers

import java.util.UUID
import javax.inject.{Named, Inject}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import domain.model.{Project, Snapshot}
import domain.repository.ProjectRepository
import domain.repository.ProjectRepository.{ProjectEmptyResponse, ProjectQuery, ProjectResponse}
import domain.repository.SnapshotRepository.{SnapshotsResponse, SnapshotsByProject}
import domain.service.SnapshotService
import domain.service.SnapshotService.CreateSnapshot
import persistence.ConnectionManager
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class Snapshots @Inject()(actorSystem: ActorSystem, manager: ConnectionManager) extends Controller {

  val projects = actorSystem.actorOf(Props[ProjectRepository], "projects")
  val snapshotService = actorSystem.actorOf(Props(classOf[SnapshotService], manager), "snapshot-service")

  implicit val timeout = Timeout(5.seconds)

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
    val project = projects ? ProjectQuery(slug = slug, one = true)
    val snapshots = snapshotService ? SnapshotsByProject(project = slug)

    project flatMap {
      case ProjectResponse(p) =>
        implicit val w = new SnapshotWriter(p)
        snapshots map { case SnapshotsResponse(s) => Ok(Json.toJson(s)) }
      case ProjectEmptyResponse() =>
        Future { NotFound(Json.obj("status" -> "ko", "message" -> s"Project '$slug' does not exist")) }
      case _ =>
        Future { InternalServerError("Gubbel!") }
    }

    //    snapshotService ? SnapshotsByProject(slug)
    //    projects findBySlug slug map {
    //      case Some(project) =>
    //        implicit val w = new SnapshotWriter(project)
    //        Ok(Json.toJson(project.snapshots))
    //      case _ => NotFound("Gubbel!")
    //    }
  }

  def create(slug: String) = Action { implicit r =>
    implicit val timeout = Timeout(5.minutes)
    snapshotService ! CreateSnapshot(slug)

    //    projects findBySlug slug foreach { case Some(p) =>
    //      p.snapshots += connectionManager.snapshot(p.slug)
    //      projects.update(p)
    //    }
    Accepted(Json.obj("status" -> "ok", "message" -> "Snapshot started"))
  }

  def restore(slug: String, id: UUID) = Action { implicit r =>
    Ok("foo")
  }

}
