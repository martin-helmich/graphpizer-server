package persistence

import domain.model.AstEdgeType._
import domain.model.AstNodeTypes._
import org.neo4j.graphdb._
import org.neo4j.graphdb.traversal.{Evaluation, Evaluator}
import play.api.Logger
import scala.collection.JavaConversions._
import scala.language.implicitConversions

object NodeWrappers {

  implicit class PathWrapper(underlyingPath: Path) {
    def end: Node = {
      underlyingPath.endNode
    }

    def --> : Node = {
      underlyingPath.endNode
    }
  }

  implicit class RelationshipWrapper(underlyingRelationship: Relationship) {

    def --> : Node = {
      underlyingRelationship.getEndNode
    }

    def end: Node = { underlyingRelationship.getEndNode }
    def start: Node = { underlyingRelationship.getStartNode }

    def apply(name: String) = property[AnyRef](name)

    def update[T](name: String, value: T) = underlyingRelationship.setProperty(name, value)

    def ?(name: String) = underlyingRelationship hasProperty name

    def property[T](name: String): Option[T] = underlyingRelationship hasProperty name match {
      case true => Some(underlyingRelationship.getProperty(name).asInstanceOf[T])
      case _ => None
    }

    def id: Long = underlyingRelationship.getId

  }

  implicit class NodeWrapper(underlyingNode: Node) {

    trait BuiltRelationship {
      def < : Relationship
      def >> : Node
    }

    trait RelationshipBuilder {
      def |-->(node: Node): BuiltRelationship
      def |-->(l: Label): BuiltRelationship

      def -->(n: Node): BuiltRelationship

      def |--(p: Map[String, AnyRef]): RelationshipBuilder
    }

    private class BuiltRelationshipImpl(relationship: Relationship) extends BuiltRelationship {
      def < = relationship
      def >> = relationship.end
    }

    private class RelationshipBuilderImpl(relationshipType: RelationshipType,
                                          source: Node,
                                          props: Map[String, AnyRef] = null,
                                          relationship: Relationship = null) extends RelationshipBuilder {

      def |-->(l: Label): BuiltRelationship = {
        val node = source.getGraphDatabase.createNode(l)
        |-->(node)
      }

      def |-->(node: Node): BuiltRelationship = {
        val rels = source >--> relationshipType filter { _.end.equals(node) }
        val rel = rels.size match {
          case 0 => source createRelationshipTo (node, relationshipType)
          case _ => rels.head
        }
        new BuiltRelationshipImpl(rel)
      }

      def -->(node: Node): BuiltRelationship = {
        val rel = source.createRelationshipTo(node, relationshipType)
        props foreach { case (key, value) => rel.setProperty(key, value) }
        new BuiltRelationshipImpl(rel)
      }

      def |--(properties: Map[String, AnyRef]): RelationshipBuilder = {
        new RelationshipBuilderImpl(relationshipType, source, properties)
      }
    }

    def apply[T <: AnyRef](name: String) = property[T](name)

    def update[T](name: String, value: T) = underlyingNode.setProperty(name, value)

    def update[T <: AnyRef](name: String, value: Option[T]): Unit = value foreach { underlyingNode.setProperty(name, _) }

    def ?(name: String) = underlyingNode hasProperty name

    def ?(label: Label) = underlyingNode hasLabel label

    def property[T](name: String): Option[T] = underlyingNode hasProperty name match {
      case true => Some(underlyingNode.getProperty(name).asInstanceOf[T])
      case _ => None
    }

    def ![T <: AnyRef](name: String): T = underlyingNode.getProperty(name).asInstanceOf[T]

    def id: Long = underlyingNode.getId

    def --|(relationshipType: RelationshipType): RelationshipBuilder = {
      new RelationshipBuilderImpl(relationshipType, underlyingNode)
    }

    def --|(relationshipTypeName: String): RelationshipBuilder = {
      val relType = DynamicRelationshipType withName relationshipTypeName
      new RelationshipBuilderImpl(relType, underlyingNode)
    }

    def <--<(relationshipType: RelationshipType): Iterable[Relationship] = {
      underlyingNode getRelationships(relationshipType, Direction.INCOMING)
    }

    def >-->(relationshipType: RelationshipType): Iterable[Relationship] = {
      underlyingNode getRelationships(relationshipType, Direction.OUTGOING)
    }

    def sub[T <: AstNode](typ: String): Option[T] = {
      (this >--> SUB filter { _.property[String]("type") exists { _ == typ } } map { _.end }).headOption.map { _.unbox.asInstanceOf[T] }
    }

    def collection[T <: AstNode](typ: String): Seq[T] = {
      val collection = (this >--> SUB filter { _.property[String]("type") exists { _ == typ } } map { _.end }).headOption
      val items = collection.map { _ >--> HAS map { _.end.unbox.asInstanceOf[T] } } map { _.toSeq }
      items.getOrElse(Seq())
    }

    def unboxTyped[T <: AstNode]: T = {
      unbox match {
        case correct: AstNode if correct.isInstanceOf[T] => correct.asInstanceOf[T]
        case _ => throw new Exception("Invalid type of node " + this)
      }
    }

    def unbox: AstNode = {
      val firstLabel = underlyingNode.getLabels.toArray.head.name
      firstLabel match {
        case "Expr_ConstFetch" => Expr_ConstFetch(underlyingNode ! "name")
        case "Expr_ArrayItem" => Expr_ArrayItem(sub[Expr]("value").getOrElse(Expr_Unknown()), sub[Expr]("key"), property[Boolean]("byRef").getOrElse(false))
        case "Expr_Array" => Expr_Array(collection[Expr_ArrayItem]("items"))
        case "Scalar_DNumber" => Scalar_DNumber(underlyingNode ! "value")
        case "Scalar_LNumber" => Scalar_LNumber(underlyingNode ! "value")
        case "Scalar_String" => Scalar_String(underlyingNode ! "value")

        case "Scalar_MagicConst_Dir" => Scalar_MagicConst_Dir()
        case "Scalar_MagicConst_Class" => Scalar_MagicConst_Class()
        case "Scalar_MagicConst_Function" => Scalar_MagicConst_Function()
        case "Scalar_MagicConst_Namespace" => Scalar_MagicConst_Namespace()
        case "Scalar_MagicConst_Trait" => Scalar_MagicConst_Trait()

        case "Expr_BinaryOp_BooleanAnd" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_BooleanOr" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_Equal" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_Greater" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_GreaterOrEqual" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_Identical" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_LogicalAnd" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_LogicalOr" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_LogicalXor" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_NotEqual" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_NotIdentical" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_Smaller" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_SmallerOrEqual" => Expr_BinaryOp_BooleanAnd(sub[Expr]("left").get, sub[Expr]("right").get)

        case "Expr_BinaryOp_BitwiseAnd" => Expr_BinaryOp_BitwiseAnd(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_BitwiseOr" => Expr_BinaryOp_BitwiseOr(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_BitwiseXor" => Expr_BinaryOp_BitwiseXor(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_ShiftLeft" => Expr_BinaryOp_ShiftLeft(sub[Expr]("left").get, sub[Expr]("right").get)
        case "Expr_BinaryOp_ShiftRight" => Expr_BinaryOp_ShiftRight(sub[Expr]("left").get, sub[Expr]("right").get)

        case "Expr_BinaryOp_Concat" => Expr_BinaryOp_Concat(sub[Expr]("left").get, sub[Expr]("right").get)

        case _ =>
          Logger.warn(s"Unknown node label $firstLabel")
          Unknown()
      }
    }

  }

  implicit def EvaluatorWrapper(e: (Path) => (Boolean, Boolean)): Evaluator = {
    new Evaluator {
      override def evaluate(path: Path): Evaluation = {
        val r = e(path)
        Evaluation.of(r._1, r._2)
      }
    }
  }

}