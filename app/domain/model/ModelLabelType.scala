package domain.model

import org.neo4j.graphdb.{Label, RelationshipType}

import scala.language.implicitConversions

object ModelLabelType extends Enumeration {
  type LabelType = Value
  val Class, Interface, Trait, Method, Type, Property, Parameter = Value

  implicit def conv(l: LabelType): Label = new Label {
    override def name(): String = l.toString
  }
}
