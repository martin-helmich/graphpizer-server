package controllers

import javax.inject.{Inject, Singleton}

import domain.model.{DataType, ModelEdgeTypes}
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
      case Some("1" | "true" | "yes" | "on") => (t: DataType) => t.collection
      case Some("0" | "false" | "no" | "off") => (t: DataType) => !t.collection
      case _ => (t: DataType) => true
    }
    val primitiveFilter = r.getQueryString("primitive") match {
      case Some("1" | "true" | "yes" | "on") => (t: DataType) => t.primitive
      case Some("0" | "false" | "no" | "off") => (t: DataType) => !t.primitive
      case _ => (t: DataType) => true
    }

    val filter = (t: DataType) => {
      collectionFilter(t) && primitiveFilter(t)
    }

    val json = manager connect project transactional { (backend, t) =>
      JsArray(
        backend execute "MATCH (t:Type) RETURN t" map { (t: Node) => DataType.fromNode(t) } filter filter map { t =>
          count += 1
          renderJson(project, t)
        }
      )
    }

    Ok(json) withHeaders (("X-ObjectCount", count.toString))
  }

  def show(project: String, typ: String) = Action { implicit r =>
    try {
      val json = manager connect project transactional { (backend, _) =>
        backend.execute("MATCH (t:Type) WHERE t.slug={id} RETURN t").params(Map("id" -> typ)).map({ (t: Node) => DataType.fromNode(t) }).headOption match {
          case None => throw new NoSuchElementException
          case Some(t) => renderJson(project, t)
        }
      }
      Ok(json)
    } catch {
      case e: NoSuchElementException => NotFound
    }
  }

  private def renderJson(project: String, t: DataType)(implicit request: Request[AnyContent]): JsValue = {
    var o = Json.obj(
      "name" -> t.name,
      "primitive" -> t.primitive,
      "collection" -> t.collection,
      "__href" -> controllers.routes.Types.show(project, t.slug).absoluteURL(),
      "__id" -> t.slug
    )

    t.inner.foreach { inner =>
      o ++= Json.obj(
        "collectionOf" -> Json.obj(
          "name" -> inner.name,
          "__href" -> controllers.routes.Types.show(project, inner.slug).absoluteURL()
        )
      )
    }

    t.classLike.foreach { classlike =>
      o ++= Json.obj(
        "isClass" -> Json.obj(
          "fqcn" -> classlike.fqcn,
          "__href" -> controllers.routes.Classes.show(project, classlike.slug).absoluteURL()
        )
      )
    }

    o
  }

}
