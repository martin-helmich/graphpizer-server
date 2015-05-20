package controllers

import javax.inject.{Singleton, Inject}

import play.api._
import play.api.mvc._

@Singleton
class Application @Inject() extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def hello(user: String) = Action { request =>
    Ok("Hello " + user)
  }

}