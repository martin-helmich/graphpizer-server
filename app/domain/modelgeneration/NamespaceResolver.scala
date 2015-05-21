package domain.modelgeneration

import org.neo4j.graphdb.traversal.{Uniqueness, Evaluation, Evaluator, Evaluators}
import org.neo4j.graphdb.{Path, Direction, Node}
import persistence.BackendInterface
import play.api.Logger
import scala.concurrent.{Await, Future}
import language.postfixOps
import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

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
          if (path.endNode().hasLabel(label)) {
            Evaluation.of(!path.endNode().hasProperty("fullName"), false)
          } else Evaluation.of(false, true)
        }
      }

      val cypher = """MATCH          (ns:Stmt_Namespace)
                      OPTIONAL MATCH (ns)-[:SUB {type: "stmts"}]->(s)-->(:Stmt_Use)-->()-->(u:Stmt_UseUse)
                      RETURN ns, collect(u) AS imports"""
      val edgeSub = backend createEdgeType "SUB"
      val edgeHas = backend createEdgeType "HAS"
      val traversal = backend
        .traversal
        .depthFirst()
        .relationships(edgeSub, Direction.OUTGOING)
        .relationships(edgeHas, Direction.OUTGOING)
        .evaluator(evaluator)
        .uniqueness(Uniqueness.NODE_GLOBAL)

      b execute cypher foreach { (ns: Node, imports: java.util.List[Node]) =>
        val namespaceName = (ns getProperty "name").asInstanceOf[String]
        val knownImports = imports map { p => (p getProperty "alias", p getProperty "name") } toMap

        ns getRelationships(edgeSub, Direction.OUTGOING) filter { r =>
          r.getProperty("type") match { case "stmts" => true case _ => false }
        } map { _.getEndNode } foreach { root =>

          traversal traverse root map { _.endNode } filter { _ hasProperty "allParts" } foreach { nameNode =>
            val name = (nameNode getProperty "allParts").asInstanceOf[String]
            knownImports get name match {
              case Some(s: String) => nameNode.setProperty("fullName", s)
              case _ => nameNode.setProperty("fullName", s"$namespaceName\\$name")
            }
          }

        }
      }

      null
    }
  }

}
