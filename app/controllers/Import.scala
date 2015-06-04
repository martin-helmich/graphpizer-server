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
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class Import @Inject()(manager: ConnectionManager, projectRepository: ProjectRepository) extends Controller {

  class BadJsonTypeException extends Exception

  val system = ActorSystem("NodeImport")
  val importer = system.actorOf(Props(classOf[NodeImportService], manager), name = "import")
  val projects = system.actorOf(Props[ProjectRepository], name = "projects")

  val ProjectNotFound = (p: String) => NotFound(Json.obj("status" -> "ko", "messsage" -> s"Project '$p' does not exist"))

  implicit val to = Timeout(1.second)

  def status(project: String) = Action.async {
    projects ? ProjectQuery(slug = project) map {
      case ProjectResponse(p) => Ok(Json.obj("status" -> "ok"))
      case ProjectEmptyResponse() => ProjectNotFound(project)
    }
  }

  def wipe(project: String) = Action.async { r =>
    projects ? ProjectQuery(slug = project) map {
      case ProjectResponse(p) =>
        importer ! WipeRequest(project)
        Accepted(Json.obj("status" -> "ok", "message" -> "Wiping all nodes"))
      case ProjectEmptyResponse() => ProjectNotFound(project)
    }
  }

  def importAst(project: String) = Action.async(BodyParsers.parse.json) { request =>
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

    projects ? ProjectQuery(slug = project) map {
      case ProjectResponse(p) =>
        val testResult = request.body.validate[ImportDataSet]
        testResult.fold(
          errors => {
            BadRequest(JsError.toFlatJson(errors))
          },
          request => {
            importer ! new NodeImportService.ImportRequest(p.slug, request)
            Accepted("Started node import")
          }
        )
      case _ => ProjectNotFound(project)
    }
  }

}
