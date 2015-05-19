package controllers

import akka.actor.{Props, ActorSystem}
import controllers.dto._
import domain.astimport.NodeImportService
import persistence.Backend
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Import extends Controller {

  val system = ActorSystem("NodeImport")
  val importer = system.actorOf(Props(classOf[NodeImportService], Backend), name = "import")

  def status(project: String) = Action {
    Ok("OK")
  }

  def importAst(project: String) = Action(BodyParsers.parse.json) { request =>
    implicit val nodeRead: Reads[Node] = (
      (JsPath \ "labels").read[Seq[String]] and
        (JsPath \ "properties").read[Map[String, JsValue]] and
        (JsPath \ "merge").readNullable[Boolean]
      )(Node.apply _)

    implicit val edgeRead: Reads[Edge] = (
      (JsPath \ "from").read[String] and
        (JsPath \ "to").read[String] and
        (JsPath \ "label").read[String] and
        (JsPath \ "properties").read[Map[String, JsValue]]
      )(Edge.apply _)

    implicit val requestRead: Reads[ImportRequest] = (
      (JsPath \ "nodes").read[Seq[Node]] and
        (JsPath \ "relationships").read[Seq[Edge]]
      )(ImportRequest.apply _)

    val testResult = request.body.validate[ImportRequest]
    testResult.fold(
      errors => {
        BadRequest(JsError.toFlatJson(errors))
      },
      request => {
        importer ! request
        Accepted("Started node import")
      }
    )
  }

}
