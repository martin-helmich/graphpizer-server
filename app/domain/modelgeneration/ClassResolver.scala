package domain.modelgeneration

import domain.astimport.DocComment.{ParamTag, ReturnTag, VarTag}
import domain.astimport.{DocComment, DocCommentParser}
import domain.model.AstNodeTypes._
import domain.model.AstEdgeType._
import domain.model.DataType
import domain.model.ModelEdgeType._
import domain.model.ModelLabelType._
import domain.modelgeneration.ClassResolver.{ImportContextImpl, ImportContext}
import org.neo4j.graphdb.{Path, Node}
import persistence.NodeWrappers._
import persistence.{BackendInterface, Query}
import play.api.Logger

import scala.collection.JavaConversions._

class ClassResolver(backend: BackendInterface, docCommentParser: DocCommentParser, typeResolver: TypeResolver) {

  private implicit val implicitBackend = backend

  private val logger = Logger

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

        val fqcn = (namespaceStmt.property[String]("name"), classStmt.property[String]("name")) match {
          case (Some(namespace: String), Some(name: String)) => namespace + "\\" + name
          case _ => classStmt.property[String]("name").get
        }

        println(s"Treating class $fqcn")

        clazz("name") = classStmt("name")
        clazz("slug") = fqcn.toLowerCase.replace("\\", "-")
        clazz("abstract") = classStmt.property[Int]("type") match {
          case Some(a: Int) => (a & 16) > 0
          case _ => false
        }
        clazz("final") = classStmt.property[Int]("type") match {
          case Some(a: Int) => (a & 32) > 0
          case _ => false
        }

        if (namespaceStmt != null) {
          clazz("namespace") = namespaceStmt("name")
          clazz("fqcn") = fqcn
        } else {
          clazz("namespace") = null
          clazz("fqcn") = clazz("name")
        }

        clazz --| DEFINED_IN |--> classStmt
        mergeDataType(DataType(fqcn, primitive = false)) --| IS |--> clazz

        val context = new ImportContextImpl(clazz, clazz.property[String]("namespace"))

        if (label != Interface) {
          extractPropertiesForClass(classStmt, clazz, context)
        }

        extractMethodsForClass(classStmt, clazz, context)
      }


      backend.execute("""MATCH (sub:Class)-[:DEFINED_IN]->(:Stmt_Class)-[:SUB {type: "extends"}]->(ename)
                         MATCH (super:Class) WHERE super.fqcn=ename.fullName
                         MERGE (sub)-[:EXTENDS]->(super)""").run().close()
      backend.execute("""MATCH (c:Class)-[:DEFINED_IN]->(:Stmt_Class)-[:SUB {type: "implements"}]->()-[:HAS]->(iname)
                         MATCH (i:Interface) WHERE i.fqcn=iname.fullName
                         MERGE (c)-[:IMPLEMENTS]->(i)""").run().close()
      backend.execute("""MATCH (c:Class)-[:DEFINED_IN]->(:Stmt_Class)-[:SUB {type: "stmts"}]->()-[:HAS]->(:Stmt_TraitUse)-[:SUB {type: "traits"}]->()-[:HAS]->(tname)
                         MATCH (t:Trait) WHERE t.fqcn = tname.fullName
                         MERGE (c)-[:USES_TRAIT]->(t)""").run().close()

      backend.execute("""MATCH (m:Method)<-[:HAS_METHOD]-()-[:IMPLEMENTS]->()-[:HAS_METHOD]->(s) WHERE m.name=s.name
                         MERGE (m)-[:IMPLEMENTS_METHOD]->(s)""").run().close()
      backend.execute("""MATCH (m:Method)<-[:HAS_METHOD]-()-[:EXTENDS]->()-[:HAS_METHOD]->(s) WHERE m.name=s.name AND m.abstract=true
                         MERGE (m)-[:IMPLEMENTS_METHOD]->(s)""").run().close()
      backend.execute("""MATCH (m:Method)<-[:HAS_METHOD]-()-[:EXTENDS]->()-[:HAS_METHOD]->(s) WHERE m.name=s.name AND m.abstract=false
                         MERGE (m)-[:OVERRIDES_METHOD]->(s)""").run().close()
    }
  }

  protected def extractPropertiesForClass(classStmt: Node, clazz: Node, context: ImportContext) = {
    val existingDefinitions = (clazz >--> HAS_PROPERTY map {
      _.end
    } map { prop => (prop.property[String]("name").get, prop) }).toMap
    val cypher = """MATCH (cls)-[:SUB|HAS*]->(outer:Stmt_Property)-->()-->(inner:Stmt_PropertyProperty) WHERE id(cls)={id}
                    OPTIONAL MATCH (inner)-[:SUB {type: "default"}]->(default)
                    RETURN outer, inner, default"""
    val p: Map[String, AnyRef] = Map("id" -> Long.box(classStmt.id))

    backend execute cypher params p foreach { (outer: Node, inner: Node, default: Node) =>
      val name = inner.property[String]("name").get
      val propNode = existingDefinitions get name match {
        case Some(existing: Node) => existing
        case _ => (clazz --| HAS_PROPERTY |--> Property).>>
      }

      val typemap = outer.property[Int]("type") match {
        case Some(a: Int) => a
        case _ => 0
      }

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
            case "true" | "false" => DataType("boolean", primitive = true)
            case _ =>
          }
          case _ => Logger.warn(s"Unknown type ${default.getLabels }")
        }

        dataTypeFromDefault match {
          case t: DataType => propNode --| POSSIBLE_TYPE |--> mergeDataType(t)
          case _ =>
        }
      }

      outer.property[String]("docComment") match {
        case Some(s: String) =>
          docCommentParser.parse(s).variable match {
            case Some(VarTag(dataTypeName, _)) =>
              dataTypeName split "\\|" foreach { typename =>
                typeResolver resolveType(typename, context) match {
                  case Some(d: DataType) => propNode --| POSSIBLE_TYPE |--> mergeDataType(d)
                  case _ =>
                }
              }
            case _ =>
          }
        case _ =>
      }
    }
  }

  protected def extractMethodsForClass(classStmt: Node, clazz: Node, context: ImportContext) = {
    val existingDefinitions = clazz >--> HAS_METHOD map { _.end } map { m => (m.property[String]("name").get, m) } toMap
    val cypher = """MATCH (cls)-[:SUB {type: "stmts"}]->()-->(m:Stmt_ClassMethod) WHERE id(cls)={cls} RETURN m"""
    val p: Map[String, AnyRef] = Map("cls" -> Long.box(classStmt.id))

    backend execute cypher params p foreach { (methodStmt: Node) =>
      val name = methodStmt.property[String]("name").get
      val methodNode = existingDefinitions get name match {
        case Some(existing: Node) => existing
        case _ => (clazz --| HAS_METHOD |--> Method).>>
      }

      val typemap: Int = methodStmt.property[Int]("type") match {
        case Some(a: Int) => a
        case _ => 0
      }

      methodNode("public") = (typemap & 1) > 0
      methodNode("protected") = (typemap & 2) > 0
      methodNode("private") = (typemap & 4) > 0
      methodNode("static") = (typemap & 8) > 0
      methodNode("abstract") = (typemap & 16) > 0
      methodNode("name") = name
      methodNode("docComment") = methodStmt("docComment")

      methodNode --| DEFINED_IN |--> methodStmt

      val methodDocComment = methodStmt.property[String]("docComment") match {
        case Some(s: String) => docCommentParser.parse(s)
        case _ => new DocComment()
      }

      methodDocComment.result match {
        case Some(ReturnTag(dataTypeName, _)) =>
          dataTypeName split "\\|" foreach { typename =>
            typeResolver resolveType(typename, context) foreach { methodNode --| POSSIBLE_TYPE |--> mergeDataType(_) }
          }
        case _ =>
      }

      val existingParams = (methodNode >--> HAS_PARAMETER map { _.end } map { p => (p.property[String]("name").get, p) }).toMap
      backend execute
        """MATCH (m)-[:SUB {type: "params"}]->()-[:HAS]->(paramDefinition) WHERE id(m)={node}
          |OPTIONAL MATCH (paramDefinition)-[:SUB {type: "default"}]->(default)
          |RETURN paramDefinition, default
        """.stripMargin params Map("node" -> Long.box(methodStmt.id)) foreach { (param: Node, default: Node) =>

        val paramName = param.property[String]("name").get
        val paramNode = existingParams get paramName match {
          case Some(existing: Node) => existing
          case _ => (methodNode --| HAS_PARAMETER |--> Parameter).>>
        }

        println(s"Param $paramName for method $name")

        paramNode("name") = paramName
        paramNode("variadic") = param.property[Boolean]("variadic") getOrElse false
        paramNode("byRef") = param.property[Boolean]("byRef") getOrElse false
        paramNode("hasDefaultValue") = default != null

        param >--> SUB filter { _.property("type") == Some("type") } filter { _.end ? "fullName" } map { _.end } foreach { (typ: Node) =>
          typ.property[String]("name") match {
            case Some("array") =>
            case _ =>
              typ.property[String]("fullName") match {
                case Some(name: String) =>
                  typeResolver resolveType (name, context) foreach { t =>
                      println(s"Possible type from type hint: ${t.name}")
                      paramNode --| POSSIBLE_TYPE |--> mergeDataType(t)
                  }
                case _ =>
              }
          }
        }

        methodDocComment param paramName match {
          case Some(ParamTag(vpn, dataTypeName: String, _)) if vpn == paramName =>
            dataTypeName split "\\|" foreach { typename =>
              typeResolver resolveType (typename, context) foreach { t =>
                println(s"Possible type from doc comment: ${t.name}")
                paramNode --| POSSIBLE_TYPE |--> mergeDataType(t)
              }
            }
          case _ => logger.warn(s"Parameter $name:$paramName is not annotated in doc block")
        }
      }
    }
  }

  protected def setNodeDataType(node: Node, typename: String, primitive: Boolean, collection: Boolean = false): Unit = {
    val q = new Query(Type, Map("name" -> typename, "primitive" -> Boolean.box(primitive), "collection" -> Boolean.box(collection)))
    val typeNode = backend.nodes.merge(q)

    node --| POSSIBLE_TYPE |--> typeNode
  }

  protected def mergeDataType(datatype: DataType): Node = {
    val typenode = backend.nodes.merge(datatype.query)

    datatype.inner match {
      case Some(t: DataType) =>
        val inner = mergeDataType(t)
        typenode --| COLLECTION_OF |--> inner
      case _ =>
    }

    typenode
  }

}

object ClassResolver {

  trait ImportContext {
    def resolveImportedName(alias: String): String
  }

  class ImportContextImpl(node: Node, currentNamespace: Option[String])(implicit backend: BackendInterface) extends ImportContext {
    val cypher = """MATCH (n)-[:DEFINED_IN]->()<-[:SUB|HAS*]-(ns:Stmt_Namespace) WHERE id(n)={node}
                    MATCH (ns)-[:SUB {type: "stmts"}]->()-->(:Stmt_Use)-->()-->(use:Stmt_UseUse)
                    RETURN use"""
    val args = Map("node" -> Long.box(node.id))
    val imports: Map[String, String] = (backend execute cypher params args map { (use: Node) => (use ! "alias", use ! "name") }).toMap

    def resolveImportedName(alias: String): String = {
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