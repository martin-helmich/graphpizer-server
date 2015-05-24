package domain.model

import org.neo4j.graphdb.RelationshipType
import language.implicitConversions

object AstEdgeType extends Enumeration {
  type EdgeType = Value
  val SUB, HAS = Value

  implicit def conv(et: EdgeType): RelationshipType = new RelationshipType {
    override def name(): String = et.toString
  }
}
