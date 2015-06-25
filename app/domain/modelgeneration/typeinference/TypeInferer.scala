package domain.modelgeneration.typeinference

import domain.mapper.TypeMapper
import persistence.BackendInterface
import play.api.Logger

class TypeInferer(backend: BackendInterface, typeMapper: TypeMapper) {

  val logger = Logger

  def run(): Unit = {
    backend transactionalDebug { (_, _) =>
      val stmts = Seq(
        """MATCH (c:Expr_New)-[:SUB{type: "class"}]->(n)
               WHERE (n:Name OR n:Name_FullyQualified) AND n.fullName IS NOT NULL
           MERGE (t:Type {name: n.fullName, primitive: false, collection: false})
           MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""",
        """MATCH (var:Expr_Variable{name: "this"})<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class:Class)<-[:IS]-(type:Type)
           MERGE (var)-[:POSSIBLE_TYPE {confidence: 1}]->(type)"""
      )

      stmts foreach { cypher =>
        Logger.info(s"Executing $cypher")
        backend.execute(cypher).run().close()
        Logger.info("done")
      }
    }

    val symbols = new SymbolTable()
    val pass = new TypeInferencePass(backend, symbols, typeMapper)

    Logger.info("Beginning iterative type inference")

    do {
      pass.pass()
      logger.info(s"Affected ${pass.affectedInLastPass} in last pass")
    } while (!pass.done)

    logger.info(s"Type inference passed after ${pass.iterationCount} passes")
    symbols.dump { s => logger.info(s) }
  }

}
