package controllers

import javax.inject.{Singleton, Inject}
import akka.actor.{Props, ActorSystem}
import controllers.dto._
import domain.astimport.NodeImportService
import domain.astimport.NodeImportService.WipeRequest
import persistence.ConnectionManager
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

@Singleton
class Import @Inject()(manager: ConnectionManager) extends Controller {

  class BadJsonTypeException extends Exception

  val system = ActorSystem("NodeImport")
  val importer = system.actorOf(Props(classOf[NodeImportService], manager), name = "import")

  def status(project: String) = Action {
    Ok("OK")
  }

  def wipe(project: String) = Action { r =>
    importer ! WipeRequest(project)
    Accepted("Wiping all nodes")
  }

  def importAst(project: String) = Action(BodyParsers.parse.json) { request =>
    val mapPrimitive: (JsValue) => Any = {
      case JsString(str) => str
      case JsNumber(no) => if (no.isValidInt) no.toInt else no.toDouble
      case JsBoolean(bo) => bo
      case JsNull => null
      case _ => throw new BadJsonTypeException
    }

    val mapPrimitiveOrArray: (JsValue) => Any = {
      case JsArray(values) =>
        if (values forall { case JsString(_) => true case _ => false }) {
          values.map { case JsString(s) => s case _ => "" }.toArray[String]
        } else {
          throw new BadJsonTypeException
        }
      case v: JsValue => mapPrimitive(v)
    }

    implicit val propertyRead = new Reads[Map[String, Any]] {
      def reads(js: JsValue): JsResult[Map[String, Any]] = {
        js match {
          case JsObject(fields) =>
            try {
              val m = fields.filter {
                case (key, JsString(_) | JsNumber(_) | JsArray(_)) => true
                case _ => false
              }.map {
                case (key, v: JsValue) => (key, mapPrimitiveOrArray(v))
              }.toMap

              JsSuccess(m)
            } catch {
              case e: BadJsonTypeException => JsError(__, ValidationError("validate.error.badtype"))
              case _: Exception => JsError(__, ValidationError("validate.error.unknown"))
            }
          case t => JsError(__, ValidationError("validate.error.badtype", t))
        }
      }
    }

    implicit val nodeRead: Reads[Node] = (
      (JsPath \ "labels").read[Seq[String]] and
        (JsPath \ "properties").read[Map[String, Any]] and
        (JsPath \ "merge").readNullable[Boolean]
      )(Node.apply _)

    implicit val edgeRead: Reads[Edge] = (
      (JsPath \ "from").read[String] and
        (JsPath \ "to").read[String] and
        (JsPath \ "label").read[String] and
        (JsPath \ "properties").read[Map[String, Any]]
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
      request => {
        importer ! new NodeImportService.ImportRequest(project, request)
        Accepted("Started node import")
      }
    )
  }

}
