package views.cypher

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
