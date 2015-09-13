package controllers

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

import javax.inject.{Inject, Singleton}

import akka.actor.{Props, ActorSystem}
import controllers.helpers.ActionHelpers._
import domain.modelgeneration.Generator.RunOptions
import domain.modelgeneration.GeneratorFactory
import domain.repository.ProjectRepository
import org.neo4j.graphdb.Label
import persistence.ConnectionManager
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import domain.model.ModelLabelTypes._

import scala.concurrent.Future

@Singleton
class Model @Inject()(manager: ConnectionManager, factory: GeneratorFactory, actorSystem: ActorSystem) extends Controller {

  val projects = actorSystem.actorOf(Props[ProjectRepository], "projects")

  def generate(project: String) = ProjectAction(project, projects) { r =>
    val options = new RunOptions(withUsage = true, withTypeInference = true)

    val generator = factory forProject r.project
    val result = Future { generator.run(options) }
    Accepted("Model generation initiated")
  }

  def show(project: String) = ProjectAction(project, projects).async {
    val b = manager.connect(project)

    val count = (l: Label) => Future {
      b transactional { (_, _) =>
        (b execute "MATCH (c:" + l.name + ") RETURN COUNT(c)" map { (c: Long) => c }).head
      }
    }

    val futures = Seq(
      count(Package),
      count(Class),
      count(Interface),
      count(Trait),
      count(Method)
    )

    Future.sequence(futures) map {
      case Seq(packages, classes, interfaces, traits, methods) =>
        Ok(
          Json.obj(
            "packages" -> packages,
            "classes" -> classes,
            "interfaces" -> interfaces,
            "traits" -> traits,
            "methods" -> methods
          )
        )
    }
  }

}
