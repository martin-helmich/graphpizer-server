package domain.modelgeneration

import domain.model.ModelEdgeType._
import domain.model.ModelLabelType._
import org.neo4j.graphdb.Direction._
import org.neo4j.graphdb.Node
import persistence.{Query, BackendInterface}

import scala.collection.JavaConversions._
import persistence.NodeWrappers._

class ClassResolver(backend: BackendInterface) {

  def run(): Unit = {
    backend transactional { (b, t) =>
      val cypher = """MATCH (cls:Stmt_Class)       OPTIONAL MATCH (ns:Stmt_Namespace)-[:SUB|HAS*]->(cls)   RETURN cls   AS cls, ns, "class"     AS type UNION
                    MATCH (iface:Stmt_Interface) OPTIONAL MATCH (ns:Stmt_Namespace)-[:SUB|HAS*]->(iface) RETURN iface AS cls, ns, "interface" AS type UNION
                    MATCH (trt:Stmt_Trait)       OPTIONAL MATCH (ns:Stmt_Namespace)-[:SUB|HAS*]->(trt)   RETURN trt   AS cls, ns, "trait"     AS type"""

      backend execute cypher foreach { (classStmt: Node, namespaceStmt: Node, kind: String) =>
        val label = kind match {
          case "class" => Class
          case "interface" => Interface
          case "trait" => Trait
        }

        val classRelations = classStmt <--< DEFINED_IN map { r => r.end } toArray
        val clazz = classRelations.length match {
          case 0 => backend.createNode(label)
          case _ => classRelations(0)
        }

        val fqcn = (namespaceStmt("name"), classStmt("name")) match {
          case (Some(namespace), Some(name)) => namespace + "\\" + name
          case _ => classStmt("name").get
        }

        println(s"Treating class $fqcn")

        clazz("name") = classStmt("name")
        clazz("abstract") = classStmt("type") match {
          case Some(a: Integer) => (a & 16) > 0
          case _ => false
        }
        clazz("final") = classStmt("type") match {
          case Some(a: Integer) => (a & 32) > 0
          case _ => false
        }

        if (namespaceStmt != null) {
          clazz("namespace") = namespaceStmt("name")
          clazz("fqcn") = fqcn
        } else {
          clazz("namespace") = null
          clazz("fqcn") = clazz("name")
        }

        if ((clazz >--> DEFINED_IN).isEmpty) {
          clazz --| DEFINED_IN |--> classStmt
        }

        val q = Query(Type, Map("name" -> fqcn, "primitive" -> Boolean.box(x = false)))
        val typeNode = backend.nodes.merge(q)

        if ((clazz <--< IS count { _.end.equals(typeNode) }) == 0) {
          typeNode --| IS |--> clazz
        }

        if (label != Interface) {
          extractPropertiesForClass(classStmt, clazz)
        }
      }
    }
  }

  protected def extractPropertiesForClass(classStmt: Node, clazz: Node) = {
    val existingDefinitions = (clazz >--> HAS_PROPERTY map { _.end } map { prop => (prop("name").get, prop) }).toMap
    val cypher = """MATCH (cls)-[:SUB|HAS*]->(outer:Stmt_Property)-->()-->(inner:Stmt_PropertyProperty) WHERE id(cls)={id}
                    OPTIONAL MATCH (inner)-[:SUB {type: "default"}]->(default)
                    RETURN outer, inner, default"""
    val p: Map[String, AnyRef] = Map("id" -> Long.box(classStmt.id))

    backend execute cypher params p foreach { (outer: Node, inner: Node, default: Node) =>
      val name = inner.property[String]("name").get
      val propNode = existingDefinitions get name match {
        case Some(existing: Node) => existing
        case _ =>
          println("Create HAS_PROPERTY relation")
          val node = backend createNode Property
          clazz --| HAS_PROPERTY |--> node
          node
      }

      println(s"Property $name")

      val typemap: Int = outer("type") match { case Some(a: Integer) => a case _ => 0 }

      propNode("name") = name
      propNode("public") = (typemap & 1) > 0
      propNode("protected") = (typemap & 2) > 0
      propNode("private") = (typemap & 4) > 0
      propNode("static") = (typemap & 8) > 0
      propNode("docComment") = outer("docComment")
    }
  }

  protected def extractMethodsForClass(classStmt: Node, clazz: Node) = {

  }

}
