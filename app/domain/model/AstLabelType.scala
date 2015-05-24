package domain.model

import org.neo4j.graphdb.Label

import scala.language.implicitConversions

object AstLabelType extends Enumeration {
  type LabelType = Value
  val Name = Value

  implicit def conv(l: LabelType): Label = new Label {
    override def name(): String = l.toString
  }
}
