package controllers

import javax.inject.{Inject, Singleton}

import domain.model.ModelEdgeType
import org.neo4j.graphdb.Node
import persistence.ConnectionManager
import persistence.NodeWrappers._
import play.api.libs.json.{JsValue, JsArray, Json}
import play.api.mvc._

@Singleton
class Types @Inject()(manager: ConnectionManager) extends Controller {

  def list(project: String) = Action { implicit r =>
    var count = 0

    val collectionFilter = r.getQueryString("collection") match {
      case Some("1" | "true" | "yes" | "on") => (t: Node) => {
        t.property[Boolean]("collection").getOrElse(false)
      }
      case Some("0" | "false" | "no" | "off") => (t: Node) => {
        !t.property[Boolean]("collection").getOrElse(false)
      }
      case _ => (t: Node) => {
        true
      }
    }
    val primitiveFilter = r.getQueryString("primitive") match {
      case Some("1" | "true" | "yes" | "on") => (t: Node) => {
        t.property[Boolean]("primitive").getOrElse(false)
      }
      case Some("0" | "false" | "no" | "off") => (t: Node) => {
        !t.property[Boolean]("primitive").getOrElse(false)
      }
      case _ => (t: Node) => {
        true
      }
    }

    val filter = (t: Node) => {
      collectionFilter(t) && primitiveFilter(t)
    }

    val json = manager connect project transactional { (backend, t) =>
      JsArray(
        backend execute "MATCH (t:Type) RETURN t" filter filter map { (t: Node) =>
          count += 1
          renderJson(project, t)
        }
      )
    }

    Ok(json) withHeaders (("X-ObjectCount", count.toString))
  }

  def show(project: String, typ: Long) = Action { implicit r =>
    try {
      val json = manager connect project transactional { (backend, t) =>
        backend execute "MATCH (t:Type) WHERE id(t)={id} RETURN t" params Map("id" -> Long.box(typ)) map { (t: Node) => renderJson(project, t) } head
      }
      Ok(json)
    } catch {
      case e: NoSuchElementException => NotFound
    }
  }

  private def renderJson(project: String, t: Node)(implicit request: Request[AnyContent]): JsValue = {
    var o = Json.obj(
      "name" -> t.property[String]("name"),
      "primitive" -> t.property[Boolean]("primitive"),
      "collection" -> t.property[Boolean]("collection"),
      "__href" -> controllers.routes.Types.show(project, t.id).absoluteURL(),
      "__id" -> t.id
    )

    val collectionOf = t >--> ModelEdgeType.COLLECTION_OF
    if (collectionOf.nonEmpty) {
      val inner = collectionOf.head.end
      o ++= Json.obj(
        "collectionOf" -> Json.obj(
          "name" -> inner.property[String]("name"),
          "__href" -> controllers.routes.Types.show(project, inner.id).absoluteURL()
        )
      )
    }

    val is = t >--> ModelEdgeType.IS
    if (is.nonEmpty) {
      val clazz = is.head.end
      o ++= Json.obj(
        "isClass" -> Json.obj(
          "fqcn" -> clazz.property[String]("fqcn"),
          "__href" -> controllers.routes.Classes.show(project, clazz ! "fqcn").absoluteURL(),
          "__id" -> clazz.id
        )
      )
    }

    o
  }

}
