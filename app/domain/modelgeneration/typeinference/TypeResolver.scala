package domain.modelgeneration.typeinference

import persistence.BackendInterface
import play.api.Logger

class TypeResolver(backend: BackendInterface) {

  val logger = Logger

  def run(): Unit = {
    backend transactional { (_, _) =>
      backend.execute("""MATCH (c:Scalar_LNumber) MERGE (t:Type {name: "integer", primitive: true})
                         MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""").run().close()
      backend.execute("""MATCH (c:Scalar_DNumber) MERGE (t:Type {name: "double", primitive: true})
                         MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""").run().close()
      backend.execute("""MATCH (c) WHERE
                             c:Scalar_String OR
                         	   c:Scalar_Encapsed OR
                             c:Scalar_MagicConst_Dir OR
                             c:Scalar_MagicConst_Class OR
                             c:Scalar_MagicConst_Function OR
                             c:Scalar_MagicConst_Namespace OR
                             c:Scalar_MagicConst_Trait
                         MERGE (t:Type {name: "string", primitive: true})
                         MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""").run().close()
      backend.execute("""MATCH (a:Expr_Array) MERGE (t:Type {name: "array", primitive: true})
                         MERGE (a)-[:POSSIBLE_TYPE]->(t)""").run().close()
      backend.execute("""MATCH (c:Expr_New)-[:SUB{type: "class"}]->(n)
                             WHERE (n:Name OR n:Name_FullyQualified) AND n.fullName IS NOT NULL
                         MERGE (t:Type {name: n.fullName, primitive: false})
                         MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""").run().close()
      backend.execute("""MATCH (var:Expr_Variable{name: "this"})<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class:Class)<-[:IS]-(type:Type)
                         MERGE (var)-[:POSSIBLE_TYPE {confidence: 1}]->(type)""").run().close()

      val symbols = new SymbolTable()
      val pass = new TypeInferencePass(backend, symbols)

      do {
        pass.pass()
        logger.info(s"Affected ${pass.affectedInLastPass} in last pass")
      } while (!pass.done)

      logger.info(s"Type inference passed after ${pass.iterationCount} passes")
    }
  }

}
