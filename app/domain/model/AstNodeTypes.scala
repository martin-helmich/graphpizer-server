package domain.model

/**
 * GraPHPizer source code analytics engine
 * Copyright (C) 2015  Martin Helmich <kontakt@martin-helmich.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object AstNodeTypes {

  class AstNode()

  class Expr() extends AstNode

  case class Expr_ConstFetch(name: String) extends Expr
  case class Expr_ArrayItem(value: Expr, key: Option[Expr] = None, byRef: Boolean = false) extends Expr
  case class Expr_Array(items: Seq[Expr_ArrayItem]) extends Expr
  case class Expr_Unknown() extends Expr

  class Scalar() extends Expr
  class MagicConst() extends Scalar

  case class Scalar_String(value: String) extends Scalar
  case class Scalar_Encapsed(parts: Seq[String]) extends Scalar
  case class Scalar_LNumber(value: Integer) extends Scalar
  case class Scalar_DNumber(value: Double) extends Scalar

  case class Scalar_MagicConst_Dir() extends MagicConst
  case class Scalar_MagicConst_Class() extends MagicConst
  case class Scalar_MagicConst_Function() extends MagicConst
  case class Scalar_MagicConst_Namespace() extends MagicConst
  case class Scalar_MagicConst_Trait() extends MagicConst

  class BinaryOp() extends Expr

  case class Expr_BinaryOp_BooleanAnd(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_BooleanOr(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_Equal(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_Greater(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_GreaterOrEqual(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_Identical(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_LogicalAnd(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_LogicalOr(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_LogicalXor(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_NotEqual(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_NotIdentical(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_Smaller(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_SmallerOrEqual(left: Expr, right: Expr) extends BinaryOp

  case class Expr_BinaryOp_BitwiseAnd(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_BitwiseOr(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_BitwiseXor(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_ShiftLeft(left: Expr, right: Expr) extends BinaryOp
  case class Expr_BinaryOp_ShiftRight(left: Expr, right: Expr) extends BinaryOp

  case class Expr_BinaryOp_Concat(left: Expr, right: Expr) extends BinaryOp

  case class Unknown() extends AstNode

}
