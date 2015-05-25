package domain.mapper

import domain.model.{Property, Class}
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

class PropertyMapper {

  def mapPropertyToNode(p: Property, n: Node) = {
    n("name") = p.name
  }
  
}
