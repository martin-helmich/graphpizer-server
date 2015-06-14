package views.cypher

import org.neo4j.graphdb.{Direction, Node, Result}
import play.api.libs.json.{JsArray, Json}
import scala.collection.JavaConversions._
import persistence.NodeWrappers._

class GraphResultView extends CypherResultView{

  import JsonImplicits._

  override def apply(result: Result, columns: Seq[String]): AnyRef = {
    val nodes = (for (row <- result; column <- columns) yield {(row get column, column) })
      .filter({ case (v: Node, c) => true case (_, _) => false })
      .map { _._1.asInstanceOf[Node] }.toSet

    val rels = nodes flatMap { node =>
      node getRelationships Direction.BOTH filter { r =>
        nodes.contains(r.end) && nodes.contains(r.start)
      }
    }

    Json.obj(
      "nodes" -> JsArray(nodes.toSeq.map { nodeToJson }),
      "rels" -> JsArray(rels.toSeq.map { relationshipToJson })
    )
  }

}
