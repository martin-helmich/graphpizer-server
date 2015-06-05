package domain.model

import org.neo4j.graphdb.Label

import scala.language.implicitConversions

object AstLabelType extends Enumeration {
  type LabelType = Value
  val File, Name, Scalar_String, Scalar_LNumber, Scalar_DNumber, Expr_Array, Expr_ConstFetch = Value

  implicit def conv(l: LabelType): Label = new Label {
    override def name(): String = l.toString
  }
}
