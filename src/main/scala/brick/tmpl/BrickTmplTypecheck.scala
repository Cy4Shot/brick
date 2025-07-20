package brick.tmpl

import brick.parse.tmpl._
import brick.parse.tmpl.Type._
import brick.parse.tmpl.Expr._

def typecheck(tmpl: TmplStmt)(using ctx: SymbolTable): Type = tmpl match {
  case IfElse(cond, ifBranch, elseBranch) =>
    verify(cond, TBool())
    val ifType = typecheck(ifBranch)
    verify(elseBranch, ifType)
    ifType
  case l: Literal => l.t
  case u: UnaryOp =>
    verify(u.e, u.input)
    u.output
  case b: SimpleBinOp =>
    verify(b.left, b.leftType)
    verify(b.right, b.rightType)
    b.expectedType
  case c: ComparisonBinOp =>
    val leftType = typecheck(c.left)
    verify(c.right, leftType)
    TBool()
  case Identifier(Ident(name)) =>
    ctx.get(name) match {
      case Some((_, ty)) => ty
      case None =>
        throw new Exception(s"Identifier ${name} not found in context")
    }
}

private def verify(expr: TmplStmt, expectedType: Type)(using
    SymbolTable
): Unit = {
  val actualType = typecheck(expr)
  if (actualType !=~ expectedType)
    throw new Exception(
      s"Expected ${expr.prettyPrint} to be ${expectedType.prettyPrint} but was ${actualType.prettyPrint}."
    )
}

extension (t1: Type)
  def ~=~(t2: Type): Boolean = (t1, t2) match {
    case (TInt(_), TInt(_))       => true
    case (TBool(_), TBool(_))     => true
    case (TChar(_), TChar(_))     => true
    case (TString(_), TString(_)) => true
    case _                        => false
  }

  def !=~(t2: Type): Boolean = !(t1 ~=~ t2)
