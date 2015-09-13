package controllers.helpers

/**
 * GraPHPizer source code analytics engine
 * Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
