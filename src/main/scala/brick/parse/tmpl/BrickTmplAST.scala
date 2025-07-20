package brick.parse.tmpl

import brick.parse.Bridges._

protected sealed trait TmplASTLeaf {
  def prettyPrint: String

  val pos: Pos = (0, 0)
}

sealed trait TmplStmt extends TmplASTLeaf

/** Represents an identifier in the AST.
  * @param ident
  *   The identifier string.
  * @param pos
  *   The position of the identifier.
  */
case class Ident(ident: String)(override val pos: Pos) extends TmplASTLeaf {
  override def prettyPrint: String = ident
}
object Ident extends ParserBridgePos1[String, Ident]

/** Represents an if-else expression in the AST.
  * @param cond
  *   The condition expression.
  * @param thenBranch
  *   The expression to evaluate if the condition is true.
  * @param elseBranch
  *   The expression to evaluate if the condition is false.
  * @param pos
  *   The position of the if-else expression.
  */
case class IfElse(
    cond: Expr.Expr,
    thenBranch: TmplStmt,
    elseBranch: TmplStmt
)(override val pos: Pos)
    extends TmplStmt {
  override def prettyPrint: String =
    s"if ${cond.prettyPrint} ? ${thenBranch.prettyPrint} : ${elseBranch.prettyPrint}"
}
object IfElse extends ParserBridgePos3[Expr.Expr, TmplStmt, TmplStmt, IfElse]

object Expr {

  /** Represents an expression in the AST.
    */
  sealed trait Expr extends TmplStmt {
    def prettyPrint: String = this match {
      case Not(e)        => s"!(${e.prettyPrint})"
      case Neg(e)        => s"-(${e.prettyPrint})"
      case Mul(x, y)     => s"(${x.prettyPrint} * ${y.prettyPrint})"
      case Div(x, y)     => s"(${x.prettyPrint} / ${y.prettyPrint})"
      case Mod(x, y)     => s"(${x.prettyPrint} % ${y.prettyPrint})"
      case Add(x, y)     => s"(${x.prettyPrint} + ${y.prettyPrint})"
      case Sub(x, y)     => s"(${x.prettyPrint} - ${y.prettyPrint})"
      case Greater(x, y) => s"(${x.prettyPrint} > ${y.prettyPrint})"
      case GreaterEq(x, y) =>
        s"(${x.prettyPrint} >= ${y.prettyPrint})"
      case Less(x, y)        => s"(${x.prettyPrint} < ${y.prettyPrint})"
      case LessEq(x, y)      => s"(${x.prettyPrint} <= ${y.prettyPrint})"
      case Eq(x, y)          => s"(${x.prettyPrint} == ${y.prettyPrint})"
      case Neq(x, y)         => s"(${x.prettyPrint} != ${y.prettyPrint})"
      case And(x, y)         => s"(${x.prettyPrint} && ${y.prettyPrint})"
      case Or(x, y)          => s"(${x.prettyPrint} || ${y.prettyPrint})"
      case IntLiteral(x)     => x.toString
      case BoolLiteral(b)    => b.toString
      case CharLiteral(c)    => s"'$c'"
      case StrLiteral(v)     => s""""$v""""
      case Identifier(ident) => ident.prettyPrint
      case _                 => ""
    }
  }

  /** Represents a binary operation in the AST.
    */
  sealed trait BinOp extends Expr {
    val left: Expr
    val right: Expr
  }

  /** Represents a simple binary operation in the AST.
    */
  sealed trait SimpleBinOp extends BinOp {
    val expectedType: Type.Type
    val leftType: Type.Type
    val rightType: Type.Type
  }

  /** Represents an arithmetic binary operation in the AST.
    */
  sealed trait ArithmeticBinOp extends SimpleBinOp {
    val expectedType = Type.TInt(pos)
    val leftType = Type.TInt(pos)
    val rightType = Type.TInt(pos)
  }

  /** Represents a boolean binary operation in the AST.
    */
  sealed trait BooleanBinOp extends SimpleBinOp {
    val expectedType = Type.TBool(pos)
    val leftType = Type.TBool(pos)
    val rightType = Type.TBool(pos)
  }

  /** Represents a comparison binary operation in the AST.
    */
  sealed trait ComparisonBinOp extends BinOp {
    var t: Option[Type.Type] = None
  }

  /** Represents an equality binary operation in the AST.
    */
  sealed trait EqualityBinOp extends BinOp {
    var t: Option[Type.Type] = None
  }

  /** Represents a unary operation in the AST.
    */
  sealed trait UnaryOp extends Expr {
    val e: Expr
    val input: Type.Type
    val output: Type.Type
  }

  /** Represents a literal value in the AST.
    * @param t
    *   The type of the literal.
    */
  abstract class Literal(val t: Type.Type) extends Expr

  /** Represents a logical NOT operation in the AST.
    * @param e
    *   The expression to negate.
    * @param pos
    *   The position of the operation.
    */
  case class Not(e: Expr)(override val pos: Pos) extends UnaryOp {
    val input = Type.TBool(pos)
    val output = Type.TBool(pos)
  }

  /** Represents a negation operation in the AST.
    * @param e
    *   The expression to negate.
    * @param pos
    *   The position of the operation.
    */
  case class Neg(e: Expr)(override val pos: Pos) extends UnaryOp {
    val input = Type.TInt(pos)
    val output = Type.TInt(pos)
  }

  /** Represents a multiplication operation in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Mul(left: Expr, right: Expr)(override val pos: Pos)
      extends ArithmeticBinOp

  /** Represents a division operation in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Div(left: Expr, right: Expr)(override val pos: Pos)
      extends ArithmeticBinOp

  /** Represents a modulo operation in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Mod(left: Expr, right: Expr)(override val pos: Pos)
      extends ArithmeticBinOp

  /** Represents an addition operation in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Add(left: Expr, right: Expr)(override val pos: Pos)
      extends ArithmeticBinOp

  /** Represents a subtraction operation in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Sub(left: Expr, right: Expr)(override val pos: Pos)
      extends ArithmeticBinOp

  /** Represents a greater-than comparison in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Greater(left: Expr, right: Expr)(override val pos: Pos)
      extends ComparisonBinOp

  /** Represents a greater-than-or-equal comparison in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class GreaterEq(left: Expr, right: Expr)(override val pos: Pos)
      extends ComparisonBinOp

  /** Represents a less-than comparison in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Less(left: Expr, right: Expr)(override val pos: Pos)
      extends ComparisonBinOp

  /** Represents a less-than-or-equal comparison in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class LessEq(left: Expr, right: Expr)(override val pos: Pos)
      extends ComparisonBinOp

  /** Represents an equality comparison in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Eq(left: Expr, right: Expr)(override val pos: Pos)
      extends EqualityBinOp

  /** Represents an inequality comparison in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Neq(left: Expr, right: Expr)(override val pos: Pos)
      extends EqualityBinOp

  /** Represents a logical AND operation in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class And(left: Expr, right: Expr)(override val pos: Pos)
      extends BooleanBinOp

  /** Represents a logical OR operation in the AST.
    * @param left
    *   The left operand.
    * @param right
    *   The right operand.
    * @param pos
    *   The position of the operation.
    */
  case class Or(left: Expr, right: Expr)(override val pos: Pos)
      extends BooleanBinOp

  /** Represents an integer literal in the AST.
    * @param x
    *   The integer value.
    * @param pos
    *   The position of the literal.
    */
  case class IntLiteral(x: Int)(override val pos: Pos)
      extends Literal(Type.TInt(pos))

  /** Represents a boolean literal in the AST.
    * @param b
    *   The boolean value.
    * @param pos
    *   The position of the literal.
    */
  case class BoolLiteral(b: Boolean)(override val pos: Pos)
      extends Literal(Type.TBool(pos))

  /** Represents a character literal in the AST.
    * @param c
    *   The character value.
    * @param pos
    *   The position of the literal.
    */
  case class CharLiteral(c: Char)(override val pos: Pos)
      extends Literal(Type.TChar(pos))

  /** Represents a string literal in the AST.
    * @param v
    *   The string value.
    * @param pos
    *   The position of the literal.
    */
  case class StrLiteral(v: String)(override val pos: Pos)
      extends Literal(Type.TString(pos))

  /** Represents an identifier expression in the AST.
    * @param ident
    *   The identifier.
    * @param pos
    *   The position of the identifier.
    */
  case class Identifier(ident: Ident)(override val pos: Pos) extends Expr

  object Not extends ParserBridgePos1[Expr, Expr]
  object Neg extends ParserBridgePos1[Expr, Expr]
  object Mul extends ParserBridgePos2[Expr, Expr, Expr]
  object Div extends ParserBridgePos2[Expr, Expr, Expr]
  object Mod extends ParserBridgePos2[Expr, Expr, Expr]
  object Add extends ParserBridgePos2[Expr, Expr, Expr]
  object Sub extends ParserBridgePos2[Expr, Expr, Expr]
  object Greater extends ParserBridgePos2[Expr, Expr, Expr]
  object GreaterEq extends ParserBridgePos2[Expr, Expr, Expr]
  object Less extends ParserBridgePos2[Expr, Expr, Expr]
  object LessEq extends ParserBridgePos2[Expr, Expr, Expr]
  object Eq extends ParserBridgePos2[Expr, Expr, Expr]
  object Neq extends ParserBridgePos2[Expr, Expr, Expr]
  object And extends ParserBridgePos2[Expr, Expr, Expr]
  object Or extends ParserBridgePos2[Expr, Expr, Expr]
  object IntLiteral extends ParserBridgePos1[Int, Expr]
  object BoolLiteral extends ParserBridgePos1[Boolean, Expr]
  object CharLiteral extends ParserBridgePos1[Char, Expr]
  object StrLiteral extends ParserBridgePos1[String, Expr]
  object Identifier extends ParserBridgePos1[Ident, Expr]
}

object Type {

  /** Represents a type in the AST.
    */
  sealed trait Type extends TmplASTLeaf {
    def prettyPrint: String =
      this match {
        case TInt(_)    => "int"
        case TBool(_)   => "bool"
        case TChar(_)   => "char"
        case TString(_) => "string"
      }
  }

  /** Represents an integer type in the AST.
    * @param pos
    *   The position of the type.
    */
  case class TInt(override val pos: Pos) extends Type

  /** Represents a boolean type in the AST.
    * @param pos
    *   The position of the type.
    */
  case class TBool(override val pos: Pos) extends Type

  /** Represents a character type in the AST.
    * @param pos
    *   The position of the type.
    */
  case class TChar(override val pos: Pos) extends Type

  /** Represents a string type in the AST.
    * @param pos
    *   The position of the type.
    */
  case class TString(override val pos: Pos) extends Type

  object TInt extends ParserBridgePos0[Type]
  object TBool extends ParserBridgePos0[Type]
  object TChar extends ParserBridgePos0[Type]
  object TString extends ParserBridgePos0[Type]
}
