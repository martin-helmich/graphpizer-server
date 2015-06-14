package domain.modelgeneration

import domain.model.DataType
import domain.modelgeneration.UsageAnalyzer.{Registry, TypeContainer}
import org.neo4j.graphdb.Node
import persistence.BackendInterface
import play.api.Logger
import domain.model.ModelEdgeType._
import persistence.NodeWrappers._

import scala.collection.mutable

class UsageAnalyzer(backend: BackendInterface) {

  def run(): Unit = {
    //val known = mutable.Map[(String, String), Boolean]()
    //val types = mutable.Map[String, Node]()
    val known = new Registry[(String, String)]()
    val types = new TypeContainer(backend)

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



    backend transactional { (_, _) =>
      val cypher =
        """MATCH (name:Name)<-[:SUB {type: "class"}]-(call:Expr_StaticCall)
          |                 <-[:SUB|HAS*]-(:Stmt_ClassMethod)
          |                 <-[:HAS]-()
          |                 <-[:SUB {type: "stmts"}]-(:Stmt_Class)
          |                 <-[:DEFINED_IN]-(c:Class)
          |WHERE call.class <> "parent" AND name.fullName IS NOT NULL
          |RETURN name.fullName, c.fqcn, c""".stripMargin

      backend execute cypher foreach { (name: String, klassName: String, klass: Node) =>
        known once(name, klassName) exec {
          val typeNode = types get name
          klass --| USES |--> typeNode
          Logger.info(klass.property[String]("fqcn").get + " --[USES]--> " + name)
        }
      }
    }

    val stmts = Seq(
      //"""MATCH (name:Name)<-[:SUB {type: "class"}]-(new:Expr_New)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(c:Class) WHERE name.fullName IS NOT NULL
      //  |MERGE (type:Type{name:name.fullName, primitive: false, collection: false})
      //  |MERGE (type)<-[:INSTANTIATES]-(new)
      //  |MERGE (c)-[r:USES]->(type) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin,
      //"""MATCH (p:Property)-[:POSSIBLE_TYPE]->(t:Type) WHERE t.primitive=false
      //  |MATCH (p)<-[:HAS_PROPERTY]-(c:Class)
      //  |MERGE (c)-[r:USES]->(t) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin,
      """MATCH (m:Method)-[:POSSIBLE_TYPE]->(t:Type) WHERE t.primitive=false
        |MATCH (m)<-[:HAS_METHOD]-(c:Class)
        |MERGE (c)-[r:USES]->(t) ON MATCH SET r.count = r.count+1 ON CREATE SET r.count=1""".stripMargin,
      """MATCH (name:Name)<-[:SUB {type: "type"}]-(p:Param)<--()<--(:Stmt_ClassMethod)<--()<-[:SUB {type: "stmts"}]-(:Stmt_Class)<-[:DEFINED_IN]-(c:Class) WHERE name.fullName IS NOT NULL AND (p.type IN ["array", "callable"]) = false
        |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
        |MERGE (type)<-[:HAS_TYPE]-(p)
        |MERGE (c)-[r:USES]->(type) ON MATCH SET r.count=r.count+1 ON CREATE SET r.count=1""".stripMargin,
      //"""MATCH (name:Name)<-[:SUB {type: "class"}]-(call:Expr_StaticCall)<-[:SUB|HAS*]-(:Stmt_ClassMethod)<-[:HAS]-()<-[:SUB {type: "stmts"}]-(:Stmt_Class)<-[:DEFINED_IN]-(c:Class) WHERE call.class <> "parent" AND name.fullName IS NOT NULL
      //  |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
      //  |MERGE (c)-[r:USES]->(type) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin,
      """MATCH (name:Name)<-[:SUB {type: "type"}]->(c:Stmt_Catch)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class)
        |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
        |MERGE (class)-[r:USES]->(type) ON MATCH SET r.count = r.count +1 ON CREATE SET r.count = 1""".stripMargin,
      """MATCH (name:Name)<-[:SUB {type: "class"}]-(:Expr_ClassConstFetch)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class) WHERE NOT (name.allParts IN ["self", "parent"])
        |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
        |MERGE (class)-[r:USES]->(type) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin
    )

    stmts foreach { cypher =>
      try {
        Logger.info(s"Executing cypher query $cypher")

        backend transactional { (_, _) =>
          backend.execute(cypher).run().close()
        }
        Logger.info("Done")
      } catch {
        case e: Exception => Logger.error(s"Exception while executing cypher statement $cypher", e)
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