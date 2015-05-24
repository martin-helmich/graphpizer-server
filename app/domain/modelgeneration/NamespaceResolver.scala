package domain.modelgeneration

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
      val cypher = """MATCH (c:Collection)-[:HAS]->(ns)-[:SUB|HAS*]->(n:Name)
                      WHERE c.fileRoot = true AND NOT ns:Stmt_Namespace
                      SET n.fullName = n.allParts"""
      b.execute(cypher).run().close()
    }
  }

  protected def treatNamespacedNodes(): Future[Unit] = {
    backend transactional { (b, t) =>
      b.execute("MATCH (name:Name_FullyQualified) SET name.fullName = name.allParts").run().close()
      b.execute("MATCH (ns:Stmt_Namespace)-[:SUB {type: \"name\"}]->(name) SET name.fullName = name.allParts").run().close()

      val label = backend createLabel "Name"
      val evaluator = new Evaluator {
        override def evaluate(path: Path): Evaluation = {
          if (path.end ? label) {
            Evaluation.of(!(path.endNode ? "fullName"), false)
          } else Evaluation.of(false, true)
        }
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
        val namespaceName = ns("name").get.asInstanceOf[String]
        val knownImports = imports map { p => (p("alias"), p("name")) } toMap

        ns >--> SUB filter { r =>
          r("type") match { case Some("stmts") => true case _ => false }
        } map { _.getEndNode } foreach { root =>

          traversal traverse root map { _.endNode } filter { _ ? "allParts" } foreach { nameNode =>
            val name = nameNode("allParts").get.asInstanceOf[String]
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
