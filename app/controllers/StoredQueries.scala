package controllers

import java.util.UUID
import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import domain.model.StoredQuery
import domain.repository.StoredQueryRepository
import domain.repository.StoredQueryRepository._
import persistence.ConnectionManager
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class StoredQueries @Inject()(actorSystem: ActorSystem, manager: ConnectionManager) extends Controller {

  val queries = actorSystem.actorOf(Props[StoredQueryRepository])

  implicit val to = Timeout(1.second)

  val QueryNotFound = (id: UUID) => NotFound(Json.obj("status" -> "ko", "messsage" -> s"Query $id does not exist"))
  val UnknownResponse = () => NotImplemented(Json.obj("status" -> "ko", "message" -> "Unknown response"))

  class QueryWrites(implicit r: Request[_]) extends Writes[StoredQuery] {
    def writes(o: StoredQuery): JsValue = Json.obj(
      "__href" -> controllers.routes.StoredQueries.show(o.id).absoluteURL(),
      "id" -> o.id,
      "cypher" -> o.cypher
    )
  }

  class NewQueryReads extends Reads[StoredQuery] {
    def reads(json: JsValue): JsResult[StoredQuery] = json match {
      case o: JsObject =>
        o \ "cypher" match {
          case JsString(s) => JsSuccess(StoredQuery(UUID.randomUUID(), s))
          case _ => JsError("cypher-missing")
        }
      case _ => JsError("not-an-object")
    }
  }

  def list() = Action.async { implicit r =>
    implicit val queryWrites = new QueryWrites()
    queries ? AllStoredQueries() map {
      case StoredQueriesResponse(qs) => Ok(Json.toJson(qs))
      case Failure(e) => InternalServerError(Json.obj("status" -> "ko", "message" -> e.getMessage))
      case _ => UnknownResponse()
    }
  }

  def show(id: UUID) = Action.async { implicit r =>
    implicit val queryWrites = new QueryWrites()
    queries ? StoredQueryById(id = id) map {
      case StoredQueryResponse(q) => Ok(Json.toJson(q))
      case EmptyResponse() => QueryNotFound(id)
      case Failure(e) => InternalServerError(Json.obj("status" -> "ko", "message" -> e.getMessage))
      case _ => UnknownResponse()
    }
  }

  def delete(id: UUID) = Action.async { implicit r =>
    queries ? StoredQueryById(id = id) flatMap {
      case StoredQueryResponse(q) => queries ? DeleteStoredQuery(q) map { _ => Ok(Json.obj("status" -> "ok")) }
      case EmptyResponse() => Future.successful(QueryNotFound(id))
      case _ => Future.successful(UnknownResponse())
    }
  }

  def create() = Action.async(BodyParsers.parse.json) { implicit r =>
    implicit val queryReads = new NewQueryReads()
    implicit val queryWrites = new QueryWrites()
    r.body.validate[StoredQuery].fold(
      errors => Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(errors)))),
      query => {
        queries ? AddStoredQuery(query) map {
          case Success(_) => Created(Json.toJson(query))
          case Failure(e) => InternalServerError(Json.obj("status" -> "ko", "message" -> e.getMessage))
          case _ => UnknownResponse()
        }
      }
    )
  }

}
