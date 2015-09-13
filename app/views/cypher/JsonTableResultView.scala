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

import org.neo4j.graphdb.{Relationship, Node, Result}
import play.api.Logger
import play.api.libs.json._
import scala.collection.JavaConversions._
import JsonImplicits._

class JsonTableResultView extends ResultView {

  override def apply(result: Result, columns: Seq[String]): AnyRef = {
    val json = result.map { r =>
      JsObject(
        columns map { name =>
          (name, r get name match {
            case n: Node => nodeToJson(n)
            case r: Relationship => relationshipToJson(r)
            case list: java.util.List[_] =>
              val containsNodes = list.count { _.isInstanceOf[Node] } == list.size
              val containsRelationships = list.count { _.isInstanceOf[Relationship] } == list.size
              val containsStrings = list.count { _.isInstanceOf[String] } == list.size

              if (containsNodes) {
                Json.obj(
                  "type" -> "node-collection",
                  "items" -> JsArray(list.toSeq.map { n => nodeToJson(n.asInstanceOf[Node]) })
                )
              } else if (containsRelationships) {
                Json.obj(
                  "type" -> "rel-collection",
                  "items" -> JsArray(list.toSeq.map { n => relationshipToJson(n.asInstanceOf[Relationship]) })
                )
              } else {
                Json.obj(
                  "type" -> "scalar-collection",
                  "items" -> JsArray(list.toSeq.map { s => JsString(s.asInstanceOf[String]) })
                )
              }
            case null => JsNull
            case s: String => JsString(s)
            case i: java.lang.Long => JsNumber(i.toLong)
            case i: java.lang.Integer => JsNumber(i.toInt)
            case d: java.lang.Double => JsNumber(d.toDouble)
            case unknown =>
              Logger.warn("Unknown Cypher column type: " + unknown.getClass)
              Json.obj("__invalid_column" -> name)
          })
        }
      )
    }.toSeq

    Json.obj(
      "columns" -> columns.map { JsString },
      "data" -> json
    )
  }

}
