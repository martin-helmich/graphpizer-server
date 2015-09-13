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

import domain.model.DataType
import domain.modelgeneration.UsageAnalyzer.{Registry, TypeContainer}
import org.neo4j.graphdb.Node
import persistence.BackendInterface
import play.api.Logger
import domain.model.ModelEdgeTypes._
import persistence.NodeWrappers._

import scala.collection.mutable

class UsageAnalyzer(backend: BackendInterface) {

  val known = new Registry[(String, String)]()
  val types = new TypeContainer(backend)

  protected val handleUsage = (typeName: String, klassName: String, klass: Node) => {
    known once(typeName, klassName) exec {
      klass --| USES |--> (types get typeName)
      Logger.info(s"$klassName --[USES]--> $typeName")
    }
  }

  def run(): Unit = {
    usagesFromConstructorCalls()
    usagesFromPropertyDefinitions()
    usagesFromStaticMethodCalls()
    usagesFromMethodDefinitions()
    usagesFromMethodParameters()
    usagesFromCatchStatements()
    usagesFromConstantUsages()

    val knownPackageDependencies = new Registry[(String, String)]()
    backend transactional { (_,_) =>
      val cypher =
        """MATCH (p1:Package)-[:CONTAINS_FILE]->(:File)
          |                  -[:HAS|SUB*]->()
          |                 <-[:DEFINED_IN]-(user)
          |                  -[:USES]->(:Type)
          |                  -[:IS]->(usee)
          |                  -[:DEFINED_IN]->()
          |                 <-[:HAS|SUB*]-(:File)
          |                 <-[:CONTAINS_FILE]-(p2:Package)
          |WHERE (user:Class OR user:Interface OR user:Trait) AND
          |      (usee:Class OR usee:Interface OR usee:Trait) AND
          |      p1 <> p2
          |RETURN p1, p2""".stripMargin

      backend execute cypher foreach { (user: Node, usee: Node) =>
        val (userName, useeName) = (user.property[String]("name").get, usee.property[String]("name").get)
        knownPackageDependencies once (userName, useeName) exec {
          user --| DEPENDS_ON |--> usee
          Logger.info(s"$userName --[DEPENDS_ON]--> $useeName")
        }
      }
    }
  }

  protected def usagesFromConstantUsages(): Unit = {
    backend transactional { (_, _) =>
      val cypher =
        """MATCH (name:Name)<-[:SUB {type: "class"}]-(:Expr_ClassConstFetch)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class) WHERE NOT (name.allParts IN ["self", "parent"])
          |RETURN name.fullName, class.fqcn, class
        """.stripMargin
      backend execute cypher foreach {
        handleUsage
      }
    }
  }

  protected def usagesFromCatchStatements(): Unit = {
    backend transactional { (_, _) =>
      val cypher =
        """MATCH (name:Name)<-[:SUB {type: "type"}]->(c:Stmt_Catch)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class)
          |RETURN name.fullName, class.fqcn, class
        """.stripMargin
      backend execute cypher foreach {
        handleUsage
      }
    }
  }

  protected def usagesFromMethodParameters(): Unit = {
    backend transactional { (_, _) =>
      val cypher =
        """MATCH (name:Name)<-[:SUB {type: "type"}]-(p:Param)
          |                 <-[:HAS]-(:Collection)
          |                 <-[:SUB {type: "params"}]-(:Stmt_ClassMethod)
          |                 <-[:HAS]-(:Collection)
          |                 <-[:SUB {type: "stmts"}]-(:Stmt_Class)
          |                 <-[:DEFINED_IN]-(c:Class)
          |WHERE name.fullName IS NOT NULL AND (p.type IN ["array", "callable"]) = false
          |RETURN name.fullName, c.fqcn, c""".stripMargin
      backend execute cypher foreach {
        handleUsage
      }
    }
  }

  def usagesFromMethodDefinitions(): Unit = {
    backend transactional { (_, _) =>
      val cypher = """MATCH (c:Class)-[:HAS_METHOD]->(m:Method)-[:POSSIBLE_TYPE]->(t:Type) WHERE t.primitive=false RETURN t, c"""
      backend execute cypher foreach { (typ: Node, klass: Node) =>
        val klassName: String = klass.property("fqcn").get
        val typName: String = typ.property("name").get

        known once(typName, klassName) exec {
          klass --| USES |--> typ
        }
      }
    }
  }

  def usagesFromStaticMethodCalls(): Unit = {
    backend transactional { (_, _) =>
      val cypher =
        """MATCH (name:Name)<-[:SUB {type: "class"}]-(call:Expr_StaticCall)
          |                 <-[:SUB|HAS*]-(:Stmt_ClassMethod)
          |                 <-[:HAS]-()
          |                 <-[:SUB {type: "stmts"}]-(:Stmt_Class)
          |                 <-[:DEFINED_IN]-(c:Class)
          |WHERE call.class <> "parent" AND name.fullName IS NOT NULL
          |RETURN name.fullName, c.fqcn, c""".stripMargin

      backend execute cypher foreach {
        handleUsage
      }
    }
  }

  def usagesFromPropertyDefinitions(): Unit = {
    backend transactional { (_, _) =>
      val cypher = """MATCH (p:Property)-[:POSSIBLE_TYPE]->(t:Type), (p)<-[:HAS_PROPERTY]-(c:Class) WHERE t.primitive=false RETURN t, c"""
      backend execute cypher foreach { (typ: Node, klass: Node) =>
        val klassName = klass.property[String]("name").get
        val typName = typ.property[String]("name").get

        known once(typName, klassName) exec {
          klass --| USES |--> typ
        }
      }
    }
  }

  def usagesFromConstructorCalls(): Unit = {
    backend transactional { (_, _) =>
      val cypher =
        """MATCH (name:Name)<-[:SUB {type: "class"}]-(new:Expr_New)
          |                 <-[:SUB|HAS*]-(:Stmt_Class)
          |                 <-[:DEFINED_IN]-(c:Class)
          |WHERE name.fullName IS NOT NULL
          |RETURN name.fullName, c.fqcn, c, new""".stripMargin

      backend execute cypher foreach { (name: String, klassName: String, klass: Node, call: Node) =>
        val typeNode = types get name
        known once(name, klassName) exec {
          klass --| USES |--> typeNode
          Logger.info(klass.property[String]("fqcn").get + " --[USES]--> " + name)
        }

        call --| INSTANTIATES |--> typeNode
        Logger.info(call.id + " --[INSTANTIATES]--> " + name)
      }
    }
  }
}

object UsageAnalyzer {

  sealed trait OnceRunner {
    def exec(fun: => Unit)
  }

  class Registry[T] {
    private val checks = mutable.Map[T, Boolean]()

    def check(key: T): Unit = checks(key) = true

    def checked(key: T): Boolean = checks.getOrElse(key, false)

    def once(key: T): OnceRunner = {
      if (!checked(key)) {
        new OnceRunner {
          def exec(fun: => Unit): Unit = {
            fun
            check(key)
          }
        }
      } else {
        new OnceRunner {
          def exec(fun: => Unit): Unit = {}
        }
      }
    }

  }

  class TypeContainer(backend: BackendInterface) {
    private val knownTypes = mutable.Map[String, Node]()

    def get(name: String): Node = {
      val op = knownTypes get name orElse {
        val typ = DataType(name, primitive = false, collection = false)
        val node = backend.nodes.merge(typ.query)
        knownTypes(name) = node
        Some(node)
      }

      op.get
    }
  }

}