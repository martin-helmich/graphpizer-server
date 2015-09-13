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

import org.neo4j.graphdb.{Direction, Node, Result}
import persistence.NodeWrappers._
import play.api.libs.json.{JsArray, Json}
import views.cypher.JsonImplicits._
import scala.collection.JavaConversions._

class DotResultView extends ResultView {
  override def apply(result: Result, columns: Seq[String]): AnyRef = {
    val nodes = (for (row <- result; column <- columns) yield {
      (row get column, column)
    })
      .filter({ case (v: Node, c) => true case (_, _) => false })
      .map {
      _._1.asInstanceOf[Node]
    }.toSet

    val rels = nodes flatMap { node =>
      node getRelationships Direction.BOTH filter { r =>
        nodes.contains(r.end) && nodes.contains(r.start)
      }
    }

    val nodeLabel = (n: Node) => {
      Seq(
        n.property[String]("name"),
        n.property[String]("fqcn")
      ) collectFirst { case Some(a) => a }
    }

    val quoteLabel = (s: String) => {
      s.replace("\\", "\\\\")
    }

    val nodesString = nodes map { node =>
      val label = nodeLabel(node) map { quoteLabel }
      "    " + node.id + label.map { l => " [label=\""+l+"\"]" }.getOrElse("")
    } mkString "\n"

    val edgesString = rels map { rel =>
      "    " + rel.start.id + " -> " + rel.end.id + " [label=\"" + rel.getType.name + "\", arrowhead=\"vee\"]"
    } mkString "\n"

    "digraph {\n    overlap=false;\n    splines=true\n" + nodesString + "\n" + edgesString + "\n}\n"
  }
}
