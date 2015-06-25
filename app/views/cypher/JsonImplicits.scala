package views.cypher

import org.neo4j.graphdb.{Relationship, Node}
import play.api.libs.json._
import persistence.NodeWrappers._
import scala.collection.JavaConversions._

import scala.language.implicitConversions

object JsonImplicits {

  protected lazy val anyRefToJsValue: (Any) => JsValue = (a: Any) => a match {
    case a: Boolean => JsBoolean(a)
    case a: Double => JsNumber(a)
    case a: Int => JsNumber(a)
    case a: String => JsString(a)
    case a: Map[String, AnyRef] => JsObject(a.toSeq.map { case (k, v) => (k, anyRefToJsValue(v)) })
    case _ => JsNull
  }

  implicit def nodeToJson(n: Node): JsValue = Json.obj(
    "id" -> n.id,
    "type" -> "node",
    "labels" -> n.getLabels.map {
      _.name
    },
    "properties" -> JsObject(n.getPropertyKeys.map { p => (p, anyRefToJsValue(n.getProperty(p))) }.toSeq)
  )

  implicit def relationshipToJson(r: Relationship): JsValue = Json.obj(
    "id" -> r.id,
    "type" -> "rel",
    "label" -> r.getType.name,
    "from" -> r.start.id,
    "to" -> r.end.id,
    "properties" -> JsObject(r.getPropertyKeys.map { p => (p, anyRefToJsValue(r.getProperty(p))) }.toSeq)
  )

}
