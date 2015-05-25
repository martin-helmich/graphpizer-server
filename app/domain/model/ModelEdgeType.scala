package domain.model

import org.neo4j.graphdb.RelationshipType
import language.implicitConversions

object ModelEdgeType extends Enumeration {
  type EdgeType = Value
  val DEFINED_IN, IS, HAS_PROPERTY, HAS_METHOD, POSSIBLE_TYPE, COLLECTION_OF = Value

  implicit def conv(et: EdgeType): RelationshipType = new RelationshipType {
    override def name(): String = et.toString
  }
}
