package controllers

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

  def generate(project: String) = Action {
    val options = new RunOptions(withUsage = true, withTypeInference = true)

    val generator = factory forProject project
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
