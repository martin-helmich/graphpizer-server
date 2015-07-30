package domain.model

import org.neo4j.graphdb.RelationshipType
import language.implicitConversions

object ModelEdgeTypes extends Enumeration {
  type EdgeType = Value
  val DEFINED_IN, IS, HAS_PROPERTY, HAS_METHOD, HAS_PARAMETER, POSSIBLE_TYPE, COLLECTION_OF, EXTENDS, IMPLEMENTS, USES_TRAIT, USES, INSTANTIATES, DEPENDS_ON = Value

  implicit def conv(et: EdgeType): RelationshipType = new RelationshipType {
    override def name(): String = et.toString
  }
}
