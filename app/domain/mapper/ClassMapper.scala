package domain.mapper

import javax.inject.Inject

import domain.model.ModelEdgeType._
import domain.model.{ModelLabelType, Class}
import org.neo4j.graphdb.Node
import persistence.NodeWrappers._

class ClassMapper @Inject() (propertyMapper: PropertyMapper) {
  
  def mapClassToNode(c: Class, n: Node) = {
    n("name") = c.name

    c.namespace foreach { ns =>
      n("namespace") = ns
      n("fqcn") = ns + "\\" + c.name
    }

    n("abstract") = c.isAbstract
    n("final") = c.isFinal

    val existingProperties = (n >--> HAS_PROPERTY map { r => (r.end.property[String]("name").get, r.end) }).toMap
    c.properties foreach { p =>
      val propertyNode = existingProperties get p.name match {
        case Some(existing: Node) => existing
        case _ => (n --| HAS_PROPERTY |--> ModelLabelType.Property).>>
      }

      propertyMapper.mapPropertyToNode(p, propertyNode)
    }
  }
  
}
