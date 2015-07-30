package controllers.helpers

import domain.model.{ClassLike, DataType}
import org.neo4j.graphdb.Node
import play.api.libs.json.{Json, JsValue}
import persistence.NodeWrappers._
import play.api.mvc.{AnyContent, Request}

object ViewHelpers {

  def writeTypeRef(p: String, t: Node)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "name" -> t.property[String]("name"),
      "__href" -> controllers.routes.Types.show(p, t.property[String]("slug").get).absoluteURL(),
      "__id" -> t.id
    )
  }

  def writeTypeRef(p: String, t: DataType)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "name" -> t.name,
      "__href" -> controllers.routes.Types.show(p, t.slug).absoluteURL()
    )
  }

  def writeClassRef(p: String, c: Node)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "fqcn" -> c.property[String]("fqcn"),
      "__href" -> controllers.routes.Classes.show(p, c ! "slug").absoluteURL(),
      "__id" -> c.id
    )
  }

  def writeClassRef(p: String, c: ClassLike)(implicit request: Request[AnyContent]): JsValue = {
    Json.obj(
      "fqcn" -> c.fqcn,
      "__href" -> controllers.routes.Classes.show(p, c.slug).absoluteURL()
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
