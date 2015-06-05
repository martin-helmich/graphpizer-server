package controllers.helpers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import domain.model.Project
import domain.repository.ProjectRepository.{ProjectQuery, ProjectResponse}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object ActionHelpers {

  class ProjectRequest[A](val project: Project, request: Request[A]) extends WrappedRequest[A](request) {

  }

  def ProjectAction(slug: String, projects: ActorRef) = new ActionRefiner[Request, ProjectRequest] with ActionBuilder[ProjectRequest] {
    def refine[A](input: Request[A]): Future[Either[Result, ProjectRequest[A]]] = {
      implicit val to = Timeout(1.second)
      projects ? ProjectQuery(slug = slug, one = true) map {
        case ProjectResponse(project) => Right(new ProjectRequest(project, input))
        case _ => Left(Results.NotFound(Json.obj("status" -> "ko", "message" -> s"Project '$slug' does not exist.")))
      }
    }
  }

}
