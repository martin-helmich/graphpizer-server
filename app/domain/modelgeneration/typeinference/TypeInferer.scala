package domain.modelgeneration.typeinference

import domain.mapper.TypeMapper
import persistence.BackendInterface
import play.api.Logger

class TypeInferer(backend: BackendInterface, typeMapper: TypeMapper) {

  val logger = Logger

  def run(): Unit = {
    backend transactional { (_, _) =>
      val stmts = Seq(
//        """MATCH (c:Scalar_LNumber) MERGE (t:Type {name: "integer", primitive: true, collection: false})
//           MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""",
//        """MATCH (c:Scalar_DNumber) MERGE (t:Type {name: "double", primitive: true, collection: false})
//           MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""",
//        """MATCH (c) WHERE
//               c:Scalar_String OR
//               c:Scalar_Encapsed OR
//               c:Scalar_MagicConst_Dir OR
//               c:Scalar_MagicConst_Class OR
//               c:Scalar_MagicConst_Function OR
//               c:Scalar_MagicConst_Namespace OR
//               c:Scalar_MagicConst_Trait
//           MERGE (t:Type {name: "string", primitive: true, collection: false})
//           MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""",
//        """MATCH (a:Expr_Array) MERGE (t:Type {name: "array", primitive: true, collection: false})
//           MERGE (a)-[:POSSIBLE_TYPE]->(t)""",
//        """MATCH (op) WHERE (op:Expr_BinaryOp_BooleanAnd OR op:Expr_BinaryOp_BooleanOr OR op:Expr_BinaryOp_Equal OR op:Expr_BinaryOp_Greater OR
//                             op:Expr_BinaryOp_GreaterOrEqual OR op:Expr_BinaryOp_Identical OR op:Expr_BinaryOp_LogicalAnd OR
//                             op:Expr_BinaryOp_LogicalOr OR op:Expr_BinaryOp_LogicalXor OR op:Expr_BinaryOp_NotEqual OR op:Expr_BinaryOp_NotIdentical OR
//                             op:Expr_BinaryOp_Smaller OR op:Expr_BinaryOp_SmallerOrEqual)
//           MERGE (t:Type {name: "boolean", primitive: true, collection: false})
//           MERGE (op)-[:POSSIBLE_TYPE]->(t)""",
//        """MATCH (op) WHERE (op:Expr_BinaryOp_BitwiseAnd OR op:Expr_BinaryOp_BitwiseOr OR op:Expr_BinaryOp_BitwiseXor OR op:Expr_BinaryOp_ShiftLeft OR op:Expr_BinaryOp_ShiftRight)
//           MERGE (t:Type {name: "integer", primitive: true, collection: false})
//           MERGE (op)-[:POSSIBLE_TYPE]->(t)""",
//        """MATCH (op) WHERE (op:Expr_BinaryOp_Concat)
//           MERGE (t:Type {name: "string", primitive: true, collection: false})
//           MERGE (op)-[:POSSIBLE_TYPE]->(t)""",

        """MATCH (c:Expr_New)-[:SUB{type: "class"}]->(n)
               WHERE (n:Name OR n:Name_FullyQualified) AND n.fullName IS NOT NULL
           MERGE (t:Type {name: n.fullName, primitive: false, collection: false})
           MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""",
        """MATCH (var:Expr_Variable{name: "this"})<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class:Class)<-[:IS]-(type:Type)
           MERGE (var)-[:POSSIBLE_TYPE {confidence: 1}]->(type)"""
      )

      stmts foreach { backend.execute(_).run().close() }

      val symbols = new SymbolTable()
      val pass = new TypeInferencePass(backend, symbols, typeMapper)

      do {
        pass.pass()
        logger.info(s"Affected ${pass.affectedInLastPass} in last pass")
      } while (!pass.done)

      logger.info(s"Type inference passed after ${pass.iterationCount} passes")
      symbols.dump { s => logger.info(s) }
    }
  }

}
