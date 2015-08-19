package views.cypher

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
