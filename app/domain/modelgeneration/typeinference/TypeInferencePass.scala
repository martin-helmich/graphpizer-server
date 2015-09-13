package domain.modelgeneration.typeinference

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

import domain.mapper.TypeMapper
import domain.model.ModelEdgeTypes._
import org.neo4j.graphdb.Node
import persistence.BackendInterface
import persistence.NodeWrappers._
import play.api.Logger
import scala.collection.JavaConversions._

class TypeInferencePass(backend: BackendInterface, symbols: SymbolTable, typeMapper: TypeMapper, maxIterationCount: Integer = 10) {

  protected var affected = -1
  protected var iter = 0

  def pass(): Unit = {
    reset()
    abortIfMaxIterationCountExceeded()

    backend transactional { (_, _) =>
      propagateExprTypeByAssignment()
      propagateExprTypeByMethodCallResult()
      propagateExprTypeByPropertyFetch()
      propagateExprTypeByBinaryOperation()
      propagateMethodTypeByReturnStatement()
      propagatePropertyTypeByAssignment()

      val cypher = """MATCH (method:Stmt_ClassMethod)<-[:SUB|HAS*]-(classStmt:Stmt_Class),
                            (classStmt)<-[:DEFINED_IN]-(class)<-[:IS]-(type)
                      OPTIONAL MATCH (method)<-[:DEFINED_IN]-()-[:HAS_PARAMETER]->(param)
                      RETURN method, classStmt, class, type, collect(param) AS parameters"""
      val variableCypher = """MATCH (c)-[:SUB|HAS*]->(var:Expr_Variable) WHERE id(c)={node} RETURN var"""
      val assignmentCypher = """MATCH (m)-[:SUB|HAS*]->(assignment:Expr_Assign)
                                         -[:SUB{type: "var"}]->(var:Expr_Variable)
                                         -[:POSSIBLE_TYPE]->(type)
                                WHERE id(m)={node}
                                RETURN var, collect(type) AS types"""

      backend execute cypher foreach { (method: Node, classStmt: Node, klass: Node, klassType: Node, parameters: java.util.List[Node]) =>
        val symbolTable = symbols
                          .scope(klass("fqcn").get)
                          .scope(method("name").get)

        backend execute variableCypher params Map("node" -> Long.box(method.id)) foreach { (variable: Node) =>
          variable.property[String]("name") match {
            case Some(name) => symbolTable.addSymbol(name)
            case _ => Logger.warn("Variable statement " + variable + " does not have a name!")
          }
        }

        symbolTable.addTypeForSymbol("this", typeMapper.mapNodeToType(klassType))

        //parameters filter { symbolTable hasSymbol _("name").get } foreach { p =>
        parameters filter { _.property[String]("name").exists { n => symbolTable.hasSymbol(n) } } foreach { p =>
          p >--> POSSIBLE_TYPE foreach { t =>
            symbolTable.addTypeForSymbol(p("name").get, typeMapper.mapNodeToType(t.end))
          }
        }

        backend execute assignmentCypher params Map("node" -> Long.box(method.id)) foreach { (variable: Node, types: java.util.List[Node]) =>
          types foreach { t =>
            variable.property[String]("name") foreach {
              symbolTable.addTypeForSymbol(_, typeMapper.mapNodeToType(t))
            }
          }
        }
      }
    }

    iter += 1
  }

  def affectedInLastPass: Int = affected

  def done: Boolean = affected == 0

  def iterationCount: Int = iter

  protected def reset(): Unit = {
    affected = 0
  }

  protected def abortIfMaxIterationCountExceeded(): Unit = {
    if (iterationCount > maxIterationCount) {
      throw new TypeInferenceLoopException
    }
  }

  protected def query(cypher: String): Unit = {
    Logger.info(s"Running cypher $cypher")
    val res = backend.execute(cypher).run()
    Logger.info("Done")
    try {
      val stats = res.getQueryStatistics

      Logger.info("Affected " + (stats.getRelationshipsCreated + stats.getNodesCreated))

      affected += stats.getRelationshipsCreated
      affected += stats.getNodesCreated
    } finally {
      res.close()
    }
  }

  protected def propagateExprTypeByAssignment(): Unit = {
    query(
      """MATCH (c:Expr_Assign)-[:SUB{type: "var"}]->(var),
               (c)-[:SUB{type: "expr"}]->(expr)-[:POSSIBLE_TYPE]->(type)
         MERGE (var)-[:POSSIBLE_TYPE]->(type)"""
    )
  }

  protected def propagateExprTypeByMethodCallResult(): Unit = {
    query(
      """MATCH (call:Expr_MethodCall)-[:SUB{type:"var"}]->(var)-[:POSSIBLE_TYPE]->(calleeType{primitive:false})
         MATCH (calleeType)-[:IS]->(:Class)-[:HAS_METHOD|EXTENDS*]->(callee:Method {name: call.name})-[:POSSIBLE_TYPE]->(calleeReturnType)
         MERGE (call)-[:POSSIBLE_TYPE]->(calleeReturnType)"""
    )
  }

  protected def propagateExprTypeByPropertyFetch(): Unit = {
    query(
      """MATCH (propFetch:Expr_PropertyFetch)-[:SUB{type: "var"}]->(var)-[:POSSIBLE_TYPE]->(parentType),
               (parentType)-[:IS]->(:Class)-[:HAS_PROPERTY|EXTENDS*]->(property:Property {name: propFetch.name})-[:POSSIBLE_TYPE]->(propertyType)
         MERGE (propFetch)-[:POSSIBLE_TYPE]->(propertyType)"""
    )
  }

  protected def propagateMethodTypeByReturnStatement(): Unit = {
    query(
      """MATCH (return:Stmt_Return)-[:SUB{type:"expr"}]->(expr)-[:POSSIBLE_TYPE]->(type:Type)
         MATCH (return)<-[:SUB|HAS*]-(methodStmt)<-[:DEFINED_IN]-(method:Method)
         MERGE (method)-[:POSSIBLE_TYPE]->(type)"""
    )
  }

  protected def propagatePropertyTypeByAssignment(): Unit = {
    query(
      """MATCH (ass:Expr_Assign)
                    -[:SUB{type:"var"}]->(propFetch:Expr_PropertyFetch)
                    -[:SUB{type:"var"}]->(propVar)
                    -[:POSSIBLE_TYPE]->(propType)
             WHERE (propFetch.name IS NOT NULL)
         MATCH (ass)-[:SUB{type: "expr"}]->(assignedExpr)
                    -[:POSSIBLE_TYPE]->(assignedType)
         MATCH (propType)-[:IS]->(propClass)
                         -[:HAS_PROPERTY|EXTENDS*]->(assignedProperty:Property{name: propFetch.name})
         MERGE (assignedProperty)-[:POSSIBLE_TYPE]->(assignedType)"""
    )
  }

  protected def propagateExprTypeByBinaryOperation(): Unit = {
    query(
      """MATCH (op)
                   -[:SUB{type: "left"}]->(left)-[:POSSIBLE_TYPE]->(leftType{name: "integer"}),
               (op)-[:SUB{type:"right"}]->(right)-[:POSSIBLE_TYPE]->(rightType{name: "integer"})
             WHERE (op:Expr_BinaryOp_Minus OR op:Expr_BinaryOp_Plus OR op:Expr_BinaryOp_Mod OR op:Expr_BinaryOp_Mul OR op:Expr_BinaryOp_Pow)
         MERGE (t:Type {name: "integer", primitive: true, collection: false})
         MERGE (op)-[:POSSIBLE_TYPE]->(t)"""
    )
    query(
      """MATCH (op:Expr_BinaryOp_Minus)
                   -[:SUB{type: "left"}]->(left)-[:POSSIBLE_TYPE]->(leftType{name: "double"}),
               (op)-[:SUB{type:"right"}]->(right)-[:POSSIBLE_TYPE]->(rightType{name: "double"})
             WHERE (op:Expr_BinaryOp_Minus OR op:Expr_BinaryOp_Plus OR op:Expr_BinaryOp_Mod OR op:Expr_BinaryOp_Mul OR op:Expr_BinaryOp_Pow)
         MERGE (t:Type {name: "double", primitive: true, collection: false})
         MERGE (op)-[:POSSIBLE_TYPE]->(t)"""
    )
  }

}
