package domain.modelgeneration.typeinference

import org.neo4j.graphdb.Node
import persistence.BackendInterface
import persistence.NodeWrappers._

class TypeInferencePass(backend: BackendInterface, symbols: SymbolTable, maxIterationCount: Integer = 10) {

  protected var affected = -1
  protected var iter = 0

  def pass(): Unit = {
    reset()
    abortIfMaxIterationCountExceeded()

    backend transactional { (_, _) =>
      propagateExprTypeByAssignment()
      propagateExprTypeByMethodCallResult()
      propagateExprTypeByPropertyFetch()
      propagateMethodTypeByReturnStatement()
      propagatePropertyTypeByAssignment()

      val cypher = """MATCH (method:Stmt_ClassMethod)<-[:SUB|HAS*]-(classStmt:Stmt_Class),
                            (classStmt)<-[:DEFINED_IN]-(class)<-[:IS]-(type)
                      OPTIONAL MATCH (method)<-[:DEFINED_IN]-()-[:HAS_PARAMETER]->(param)
                      RETURN method, classStmt, class, type, collect(param) AS parameters"""
      backend execute cypher foreach { (method: Node, classStmt: Node, klass: Node, datatype: Node, parameters: Seq[Node]) =>
        val symbolTable = symbols
          .scope(klass.property[String]("fqcn").get)
          .scope(method.property[String]("name").get)
      }
    }

    iter += 1
  }

  def affectedInLastPass: Int = {
    affected
  }

  def done: Boolean = {
    affected == 0
  }

  def iterationCount: Int = {
    iter
  }

  protected def reset(): Unit = {
    affected = 0
  }

  protected def abortIfMaxIterationCountExceeded(): Unit = {
    if (iterationCount > maxIterationCount) {
      throw new TypeInferenceLoopException
    }
  }

  protected def query(cypher: String): Unit = {
    val res = backend.execute(cypher).run()
    try {
      val stats = res.getQueryStatistics

      affected += stats.getRelationshipsCreated
      affected += stats.getNodesCreated
    } finally {
      res.close()
    }
  }

  protected def propagateExprTypeByAssignment(): Unit = {
    query( """MATCH (c:Expr_Assign)-[:SUB{type: "var"}]->(var),
                   (c)-[:SUB{type: "expr"}]->(expr)-[:POSSIBLE_TYPE]->(type)
             MERGE (var)-[:POSSIBLE_TYPE]->(type)""")
  }

  protected def propagateExprTypeByMethodCallResult(): Unit = {
    query( """MATCH (call:Expr_MethodCall)-[:SUB{type:"var"}]->(var)-[:POSSIBLE_TYPE]->(calleeType{primitive:false})
             MATCH (calleeType)-[:IS]->(:Class)-[:HAS_METHOD|EXTENDS*]->(callee:Method {name: call.name})-[:POSSIBLE_TYPE]->(calleeReturnType)
             MERGE (call)-[:POSSIBLE_TYPE]->(calleeReturnType)""")
  }

  protected def propagateExprTypeByPropertyFetch(): Unit = {
    query( """MATCH (propFetch:Expr_PropertyFetch)-[:SUB{type: "var"}]->(var)-[:POSSIBLE_TYPE]->(parentType),
                   (parentType)-[:IS]->(:Class)-[:HAS_PROPERTY|EXTENDS*]->(property:Property {name: propFetch.name})-[:POSSIBLE_TYPE]->(propertyType)
             MERGE (propFetch)-[:POSSIBLE_TYPE]->(propertyType)""")
  }

  protected def propagateMethodTypeByReturnStatement(): Unit = {
    query( """MATCH (return:Stmt_Return)-[:SUB{type:"expr"}]->(expr)-[:POSSIBLE_TYPE]->(type:Type)
             MATCH (return)<-[:SUB|HAS*]-(methodStmt)<-[:DEFINED_IN]-(method:Method)
             MERGE (method)-[:POSSIBLE_TYPE]->(type)""")
  }

  protected def propagatePropertyTypeByAssignment(): Unit = {
    query( """MATCH (ass:Expr_Assign)
                       -[:SUB{type:"var"}]->(propFetch:Expr_PropertyFetch)
                       -[:SUB{type:"var"}]->(propVar)
                       -[:POSSIBLE_TYPE]->(propType)
                 WHERE (propFetch.name IS NOT NULL)
             MATCH (ass)
                       -[:SUB{type: "expr"}]->(assignedExpr)
                       -[:POSSIBLE_TYPE]->(assignedType)
             MATCH (propType)
                       -[:IS]->(propClass)
                       -[:HAS_PROPERTY|EXTENDS*]->(assignedProperty:Property{name: propFetch.name})
             MERGE (assignedProperty)-[:POSSIBLE_TYPE]->(assignedType)""")
  }

}
