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
import domain.model.Project
import persistence.BackendInterface
import play.api.Logger

class TypeInferer(backend: BackendInterface, typeMapper: TypeMapper, project: Project) {

  val logger = Logger

  def run(): Unit = {
    println(project)

    backend transactionalDebug { (_, _) =>
      val stmts = Seq(
        """MATCH (c:Expr_New)-[:SUB{type: "class"}]->(n)
               WHERE (n:Name OR n:Name_FullyQualified) AND n.fullName IS NOT NULL
           MERGE (t:Type {name: n.fullName, primitive: false, collection: false})
           MERGE (c)-[:POSSIBLE_TYPE {confidence: 1}]->(t)""",
        """MATCH (var:Expr_Variable{name: "this"})<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class:Class)<-[:IS]-(type:Type)
           MERGE (var)-[:POSSIBLE_TYPE {confidence: 1}]->(type)"""
      )
      val additionalStmts = project additionalStmts "preTypeInference" map { _.cypher }

      (stmts ++ additionalStmts) foreach { cypher =>
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
