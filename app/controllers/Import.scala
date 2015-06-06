package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import controllers.dto._
import controllers.helpers.JsonHelpers.JsonObjectReads
import domain.astimport.NodeImportService
import domain.astimport.NodeImportService.WipeRequest
import domain.repository.ProjectRepository
import domain.repository.ProjectRepository._
import persistence.ConnectionManager
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import controllers.helpers.ActionHelpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class Import @Inject()(manager: ConnectionManager, actorSystem: ActorSystem) extends Controller {

  class BadJsonTypeException extends Exception

  val importer = actorSystem.actorOf(Props(classOf[NodeImportService], manager), name = "import")
  val projects = actorSystem.actorOf(Props[ProjectRepository], name = "projects")

  val ProjectNotFound = (p: String) => NotFound(Json.obj("status" -> "ko", "messsage" -> s"Project '$p' does not exist"))

  implicit val to = Timeout(1.second)

  def status(project: String) = Action.async {
    projects ? ProjectQuery(slug = project, one = true) map {
      case ProjectResponse(p) => Ok(Json.obj("status" -> "ok"))
      case ProjectEmptyResponse() => ProjectNotFound(project)
    }
  }

  def wipe(project: String) = ProjectAction(project, projects).async { r =>
    implicit val to = Timeout(10.minutes)
    importer ? WipeRequest(r.project.slug) map { r =>
      Ok(Json.obj("status" -> "ok"))
    }
  }

  def importAst(project: String) = ProjectAction(project, projects)(BodyParsers.parse.json) { request =>
    implicit val objectReads = new JsonObjectReads()
    implicit val nodeRead: Reads[Node] = (
      (JsPath \ "labels").read[Seq[String]] and
        (JsPath \ "properties").read[Map[String, AnyRef]] and
        (JsPath \ "merge").readNullable[Boolean]
      )(Node.apply _)

    implicit val edgeRead: Reads[Edge] = (
      (JsPath \ "from").read[String] and
        (JsPath \ "to").read[String] and
        (JsPath \ "label").read[String] and
        (JsPath \ "properties").read[Map[String, AnyRef]]
      )(Edge.apply _)

    implicit val requestRead: Reads[ImportDataSet] = (
      (JsPath \ "nodes").read[Seq[Node]] and
        (JsPath \ "relationships").read[Seq[Edge]]
      )(ImportDataSet.apply _)

      val testResult = request.body.validate[ImportDataSet]
      testResult.fold(
        errors => {
          BadRequest(JsError.toFlatJson(errors))
        },
        importRequest => {
          importer ! new NodeImportService.ImportRequest(request.project.slug, importRequest)
          Accepted(Json.obj("status" -> "ok", "message" -> "Started node import"))
        }
      )
  }

}
