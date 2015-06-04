package controllers

import javax.inject.{Singleton, Inject}

import org.neo4j.graphdb._
import persistence.ConnectionManager
import play.api.libs.json._
import play.api.mvc.{BodyParsers, Action, Controller}
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import persistence.NodeWrappers._

@Singleton
class Cypher @Inject()(manager: ConnectionManager) extends Controller {

  import Cypher._
  import controllers.helpers.JsonHelpers._

  type CypherView = (Result, Seq[String]) => JsValue

  protected lazy val anyRefToJsValue: (Any) => JsValue = (a: Any) => a match {
    case a: Boolean => JsBoolean(a)
    case a: Double => JsNumber(a)
    case a: Int => JsNumber(a)
    case a: String => JsString(a)
    case a: Map[String, AnyRef] => JsObject(a.toSeq.map { case (k, v) => (k, anyRefToJsValue(v)) })
    case _ => JsNull
  }

  protected val mapNode = (n: Node) => Json.obj(
    "id" -> n.id,
    "type" -> "node",
    "labels" -> n.getLabels.map { _.name },
    "properties" -> JsObject(n.getPropertyKeys.map { p => (p, anyRefToJsValue(n.getProperty(p))) }.toSeq)
  )

  protected val mapRel = (r: Relationship) => Json.obj(
    "id" -> r.id,
    "type" -> "rel",
    "label" -> r.getType.name,
    "start" -> r.start.id,
    "end" -> r.end.id,
    "properties" -> JsObject(r.getPropertyKeys.map { p => (p, anyRefToJsValue(r.getProperty(p))) }.toSeq)
  )

  protected val graphView: CypherView = (result, columns) => {
    val nodes = (for (row <- result; column <- columns) yield {(row get column, column) })
                .filter({ case (v: Node, c) => true case (_, _) => false })
                .map { _._1.asInstanceOf[Node] }.toSet

    val rels = nodes flatMap { node =>
      node getRelationships Direction.BOTH filter { r =>
        nodes.contains(r.end) && nodes.contains(r.start)
      }
    }

    Json.obj(
      "nodes" -> JsArray(nodes.toSeq.map { mapNode }),
      "rels" -> JsArray(rels.toSeq.map { mapRel })
    )
  }

  protected val tableView: CypherView = (result, columns) => {
    val json = result.map { r =>
      JsObject(
        columns map { name =>
          (name, r get name match {
            case n: Node => mapNode(n)
            case r: Relationship => mapRel(r)
            case nodes: java.util.List[Node] => Json.obj(
              "type" -> "node-collection",
              "items" -> JsArray(nodes.toSeq.map { mapNode })
            )
            case rels: java.util.List[Relationship] => Json.obj(
              "type" -> "rel-collection",
              "items" -> JsArray(rels.toSeq.map { mapRel })
            )
            case null => JsNull
            case s: String => JsString(s)
            case i: java.lang.Long => JsNumber(i.toLong)
            case i: java.lang.Integer => JsNumber(i.toInt)
            case d: java.lang.Double => JsNumber(d.toDouble)
            case blub => println(blub); println(blub.getClass); Json.obj("whut" -> "??")
          })
        }
      )
    }.toSeq

    Json.obj(
      "columns" -> columns.map { JsString },
      "data" -> json
    )
  }

  def execute(project: String) = Action.async(BodyParsers.parse.json) { request =>
    Future {
      implicit val objectReads = new JsonObjectReads()
      implicit val reads = Json.reads[Query]
      request.body.validate[Query].fold(
        errors => {BadRequest(JsError.toFlatJson(errors)) },
        query => {
          try {
            val view = query.graph match {
              case Some(true) => graphView
              case _ => tableView
            }

            manager connect project transactional { (b, _) =>
              val result = b execute query.cypher run()
              val columns = result.columns()

              val json = view(result, columns)

              Ok(Json.toJson(json))
            }
          } catch {
            case e: QueryExecutionException => BadRequest(Json.obj("status" -> "ko", "message" -> e.getMessage))
          }
        }
      )
    }
  }

}

object Cypher {

  case class Query(cypher: String, params: Map[String, AnyRef] = Map(), graph: Option[Boolean] = None)

}