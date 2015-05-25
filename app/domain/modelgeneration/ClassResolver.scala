package domain.modelgeneration

import domain.astimport.DocComment.VarTag
import domain.astimport.DocCommentParser
import domain.model.AstNodeTypes._
import domain.model.DataType
import domain.model.ModelEdgeType._
import domain.model.ModelLabelType._
import domain.modelgeneration.ClassResolver.{ImportContextImpl, ImportContext}
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._
import persistence.{BackendInterface, Query}
import play.api.Logger

import scala.collection.JavaConversions._

class ClassResolver(backend: BackendInterface, docCommentParser: DocCommentParser, typeResolver: TypeResolver) {

  private implicit val implicitBackend = backend

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

        val classRelations = (classStmt <--< DEFINED_IN map { r => r.end }).toArray
        val clazz = classRelations.length match {
          case 0 => backend.createNode(label)
          case _ => classRelations(0)
        }

        val fqcn = (namespaceStmt("name"), classStmt("name")) match {
          case (Some(namespace), Some(name)) => namespace + "\\" + name
          case _ => classStmt.property[String]("name").get
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

//        if ((clazz >--> DEFINED_IN).isEmpty) {
        clazz --| DEFINED_IN |--> classStmt
//        }

        val typeNode = mergeDataType(DataType(fqcn, primitive = false))

//        if ((clazz <--< IS count { _.end.equals(typeNode) }) == 0) {
        typeNode --| IS |--> clazz
//        }

        val context = new ImportContextImpl(clazz, clazz.property[String]("namespace"))

        if (label != Interface) {
          extractPropertiesForClass(classStmt, clazz, context)
        }
      }
    }
  }

  protected def extractPropertiesForClass(classStmt: Node, clazz: Node, context: ImportContext) = {
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
          val node = backend createNode Property
          clazz --| HAS_PROPERTY |--> node
          node
      }

      val typemap: Int = outer("type") match { case Some(a: Integer) => a case _ => 0 }

      propNode("name") = name
      propNode("public") = (typemap & 1) > 0
      propNode("protected") = (typemap & 2) > 0
      propNode("private") = (typemap & 4) > 0
      propNode("static") = (typemap & 8) > 0
      propNode("docComment") = outer("docComment")

      if (default != null) {
        val dataTypeFromDefault = default.unbox match {
          case Scalar_String(_) => DataType("string", primitive = true)
          case Scalar_LNumber(_) => DataType("integer", primitive = true)
          case Scalar_DNumber(_) => DataType("double", primitive = true)
          case Expr_ConstFetch(constName) => constName.toLowerCase match {
            case "true"|"false" => DataType("boolean", primitive = true)
            case _ =>
          }
          case _ => Logger.warn(s"Unknown type ${default.getLabels}")
        }

        dataTypeFromDefault match {
          case t: DataType => propNode --| POSSIBLE_TYPE |--> mergeDataType(t)
          case _ =>
        }
      }

      outer("docComment") match {
        case Some(s: String) =>
          docCommentParser.parse(s).variable match {
            case Some(VarTag(dataTypeName, _)) =>
              dataTypeName split "\\|" foreach { typename =>
                val dataType = typeResolver resolveType (typename, context)
                propNode --| POSSIBLE_TYPE |--> mergeDataType(dataType)
              }
            case _ =>
          }
        case _ =>
      }
    }
  }

  protected def extractMethodsForClass(classStmt: Node, clazz: Node) = {

  }

  protected def setNodeDataType(node: Node, typename: String, primitive: Boolean, collection: Boolean = false): Unit = {
    val q = new Query(Type, Map("name" -> typename, "primitive" -> Boolean.box(primitive), "collection" -> Boolean.box(collection)))
    val typeNode = backend.nodes.merge(q)

    node --| POSSIBLE_TYPE |--> typeNode
  }

  protected def mergeDataType(datatype: DataType): Node = {
    val typenode = backend.nodes.merge(datatype.query)

    if (datatype.inner != null) {
      val inner = mergeDataType(datatype.inner)
      typenode --| COLLECTION_OF |--> inner
    }

    typenode
  }

}

object ClassResolver {

  trait ImportContext {
    def resolveImportedName(alias: String) : String
  }

  class ImportContextImpl(node: Node, currentNamespace: Option[String])(implicit backend: BackendInterface) extends ImportContext {
    val cypher = """MATCH (n)-[:DEFINED_IN]->()<-[:SUB|HAS*]-(ns:Stmt_Namespace) WHERE id(n)={node}
                    MATCH (ns)-[:SUB {type: "stmts"}]->()-->(:Stmt_Use)-->()-->(use:Stmt_UseUse)
                    RETURN use"""
    val args = Map("node" -> Long.box(node.id))
    val imports: Map[String, String] = (backend execute cypher params args map { (use: Node) => (use ! "alias", use ! "name") }).toMap

    def resolveImportedName(alias: String) : String = {
      if (alias startsWith "\\") {
        alias
      } else if (alias stripPrefix "\\" startsWith currentNamespace.getOrElse("-")) {
        alias
      } else {
        imports get alias match {
          case Some(classname: String) => classname
          case _ => currentNamespace match {
            case Some(namespace) => s"$namespace\\$alias"
            case _ => alias
          }
        }
      }
    }
  }

}