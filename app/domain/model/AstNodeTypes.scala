package domain.model

object AstNodeTypes {

  class AstNode()

  class Expr() extends AstNode

  case class Expr_ConstFetch(name: String) extends Expr
  case class Expr_Array() extends Expr

  class Scalar() extends Expr

  case class Scalar_String(value: String) extends Scalar
  case class Scalar_LNumber(value: Integer) extends Scalar
  case class Scalar_DNumber(value: Double) extends Scalar

  case class Unknown() extends AstNode

}
