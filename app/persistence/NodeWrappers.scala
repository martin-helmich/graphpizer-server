package persistence

import org.neo4j.graphdb._
import scala.collection.JavaConversions._

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

    def end: Node = {
      underlyingRelationship.getEndNode
    }

    def apply(name: String) = property[AnyRef](name)

    def update[T](name: String, value: T) = underlyingRelationship.setProperty(name, value)

    def ?(name: String) = underlyingRelationship hasProperty name

    def property[T](name: String): Option[T] = underlyingRelationship hasProperty name match {
      case true => Some(underlyingRelationship.getProperty(name).asInstanceOf[T])
      case _ => None
    }

  }

  implicit class NodeWrapper(underlyingNode: Node) {

    trait BuiltRelationship {
      def < : Relationship
    }

    trait RelationshipBuilder {
      def |-->(node: Node): BuiltRelationship

      def -->(n: Node): BuiltRelationship

      def |--(p: Map[String, AnyRef]): RelationshipBuilder
    }

    private class BuiltRelationshipImpl(relationship: Relationship) extends BuiltRelationship {
      def < = relationship
    }

    private class RelationshipBuilderImpl(relationshipType: RelationshipType,
                                          source: Node,
                                          props: Map[String, AnyRef] = null,
                                          relationship: Relationship = null) extends RelationshipBuilder {

      def |-->(node: Node): BuiltRelationship = {
        val rel = source.createRelationshipTo(node, relationshipType)
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

    def apply(name: String) = property[AnyRef](name)

    def update[T](name: String, value: T) = underlyingNode.setProperty(name, value)

    def update[T](name: String, value: Option[T]) = value match {
      case Some(v: T) => underlyingNode.setProperty(name, v)
      case _ =>
    }

    def ?(name: String) = underlyingNode hasProperty name

    def ?(label: Label) = underlyingNode hasLabel label

    def property[T](name: String): Option[T] = underlyingNode hasProperty name match {
      case true => Some(underlyingNode.getProperty(name).asInstanceOf[T])
      case _ => None
    }

    def id: Long = underlyingNode.getId

    //    def property[T](name: String): T = {
    //      underlyingNode.getProperty(name).asInstanceOf[T]
    //    }

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

  }

}