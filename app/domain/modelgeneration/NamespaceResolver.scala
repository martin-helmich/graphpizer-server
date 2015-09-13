package domain.modelgeneration

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

import domain.model.AstEdgeType._
import org.neo4j.graphdb.traversal.{Evaluation, Evaluator, Uniqueness}
import org.neo4j.graphdb.{Direction, Node, Path}
import persistence.BackendInterface
import play.api.Logger

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import persistence.NodeWrappers._

class NamespaceResolver(backend: BackendInterface) {

  protected val logger = Logger("namespace-resolver")

  def run(): Unit = {
    val f1 = Future { treatNamespacedNodes() }
    val f2 = Future { treatUnnamespacedNodes() }

    Await.result(f1, Duration.Inf)
    Await.result(f2, Duration.Inf)
  }

  protected def treatUnnamespacedNodes(): Unit = {
    backend transactional { (b, t) =>
      val cypher = """MATCH (c:Collection:File)-[:HAS]->(ns)-[:SUB|HAS*]->(n:Name)
                      WHERE NOT ns:Stmt_Namespace
                      SET n.fullName = n.allParts"""
      b.execute(cypher).run().close()
    }
  }

  protected def treatNamespacedNodes(): Future[Unit] = {
    backend transactional { (b, t) =>
      b.execute("MATCH (name:Name_FullyQualified) SET name.fullName = name.allParts").run().close()
      b.execute("MATCH (ns:Stmt_Namespace)-[:SUB {type: \"name\"}]->(name) SET name.fullName = name.allParts").run().close()

      val label = backend createLabel "Name"
      val evaluator = (path: Path) => {
        if (path.end ? label) {
          (!(path.endNode ? "fullName"), false)
        } else (false, true)
      }

      val cypher = """MATCH          (ns:Stmt_Namespace)
                      OPTIONAL MATCH (ns)-[:SUB {type: "stmts"}]->(s)-->(:Stmt_Use)-->()-->(u:Stmt_UseUse)
                      RETURN ns, collect(u) AS imports"""
      val traversal = backend
        .traversal
        .depthFirst()
        .relationships(SUB, Direction.OUTGOING)
        .relationships(HAS, Direction.OUTGOING)
        .evaluator(evaluator)
        .uniqueness(Uniqueness.NODE_GLOBAL)

      b execute cypher foreach { (ns: Node, imports: java.util.List[Node]) =>
        val Some(namespaceName) = ns[String]("name")
        val knownImports = imports map { p => (p.property[String]("alias").get, p.property[String]("name").get) } toMap

        ns >--> SUB filter { r =>
          r("type") match { case Some("stmts") => true case _ => false }
        } map { _.getEndNode } foreach { root =>

          traversal traverse root map { _.endNode } filter { _ ? "allParts" } foreach { nameNode =>
            val Some(name) = nameNode[String]("allParts")
            knownImports get name match {
              case Some(s: String) => nameNode("fullName") = s
              case _ => nameNode("fullName") = s"$namespaceName\\$name"
            }
          }

        }
      }

      null
    }
  }

}
