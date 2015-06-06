package domain.modelgeneration

import persistence.BackendInterface

class UsageAnalyzer(backend: BackendInterface) {

  def run(): Unit = {
    backend transactional { (_,_) =>
      val stmts = Seq(
        """MATCH (name)<-[:SUB {type: "class"}]-(new:Expr_New)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(c:Class) WHERE name.fullName IS NOT NULL
          |MERGE (type:Type{name:name.fullName, primitive: false, collection: false})
          |MERGE (type)<-[:INSTANTIATES]-(new)
          |MERGE (c)-[r:USES]->(type) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin,
        """MATCH (p:Property)-[:POSSIBLE_TYPE]->(t) WHERE t.primitive=false
          |MATCH (p)<-[:HAS_PROPERTY]-(c:Class)
          |MERGE (c)-[r:USES]->(t) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin,
        """MATCH (m:Method)-[:POSSIBLE_TYPE]->(t) WHERE t.primitive=false
          |MATCH (m)<-[:HAS_METHOD]-(c:Class)
          |MERGE (c)-[r:USES]->(t) ON MATCH SET r.count = r.count+1 ON CREATE SET r.count=1""".stripMargin,
        """MATCH (name)<-[:SUB {type: "type"}]-(p:Param)<--()<--(:Stmt_ClassMethod)<--()<-[:SUB {type: "stmts"}]-(:Stmt_Class)<-[:DEFINED_IN]-(c:Class) WHERE name.fullName IS NOT NULL AND (p.type IN ["array", "callable"]) = false
          |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
          |MERGE (type)<-[:HAS_TYPE]-(p)
          |MERGE (c)-[r:USES]->(type) ON MATCH SET r.count=r.count+1 ON CREATE SET r.count=1""".stripMargin,
        """MATCH (name:Name)<-[:SUB {type: "class"}]-(call:Expr_StaticCall)<-[:SUB|HAS*]-(:Stmt_ClassMethod)<-[:HAS]-()<-[:SUB {type: "stmts"}]-(:Stmt_Class)<-[:DEFINED_IN]-(c:Class) WHERE call.class <> "parent" AND name.fullName IS NOT NULL
          |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
          |MERGE (c)-[r:USES]->(type) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin,
        """MATCH (name)<-[:SUB {type: "type"}]->(c:Stmt_Catch)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class)
          |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
          |MERGE (class)-[r:USES]->(type) ON MATCH SET r.count = r.count +1 ON CREATE SET r.count = 1""".stripMargin,
        """MATCH (name)<-[:SUB {type: "class"}]-(:Expr_ClassConstFetch)<-[:SUB|HAS*]-(:Stmt_Class)<-[:DEFINED_IN]-(class) WHERE NOT (name.allParts IN ["self", "parent"])
          |MERGE (type:Type {name: name.fullName, primitive: false, collection: false})
          |MERGE (class)-[r:USES]->(type) ON MATCH SET r.count = r.count + 1 ON CREATE SET r.count = 1""".stripMargin
      )

      stmts foreach { backend.execute(_).run().close() }
    }
  }
}
