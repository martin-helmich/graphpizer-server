package controllers.helpers

import org.neo4j.graphdb.Node
import play.api.libs.json.{Json, JsValue}
import persistence.NodeWrappers._
import play.api.mvc.{AnyContent, Request}

object ViewHelpers {

  def writeTypeRef(p: String, t: Node)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "name" -> t.property[String]("name"),
      "__href" -> controllers.routes.Types.show(p, t ! "slug").absoluteURL(),
      "__id" -> t.id
    )
  }

  def writeClassRef(p: String, c: Node)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "fqcn" -> c.property[String]("fqcn"),
      "__href" -> controllers.routes.Types.show(p, c ! "slug").absoluteURL(),
      "__id" -> c.id
    )
  }

  def writeInterfaceRef(p: String, i: Node)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "fqcn" -> i.property[String]("fqcn"),
      "__href" -> controllers.routes.Interfaces.show(p, i ! "slug").absoluteURL(),
      "__id" -> i.id
    )
  }

}
