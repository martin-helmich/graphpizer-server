package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import domain.modelgeneration.GeneratorFactory
import persistence.ConnectionManager
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class Model @Inject()(manager: ConnectionManager, factory: GeneratorFactory) extends Controller {

  val system = ActorSystem("model-generation")

  def generate(project: String) = Action {
    val generator = factory forProject project
    val result = Future { generator.run() }
    Accepted("Model generation initiated")
  }

}
