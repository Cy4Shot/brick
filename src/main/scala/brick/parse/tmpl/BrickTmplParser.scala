package brick.parse.tmpl

import parsley.{Parsley, Result}
import parsley.expr._
import parsley.errors.combinator._
import parsley.errors.patterns.PreventativeErrors
import parsley.quick.notFollowedBy

import brick.parse.tmpl._
import brick.parse.tmpl.Expr._
import brick.parse.tmpl.BrickTmplLexer._
import brick.parse.tmpl.BrickTmplLexer.implicits.implicitSymbol

def parseExpr(input: String): Result[String, Expr] =
    parser.parse(input)

private lazy val parser = fully(expr)

private lazy val expr: Parsley[Expr] =
  precedence(
    IntLiteral(integer).label("integer literal"),
    BoolLiteral("true" #> true | "false" #> false).label("boolean literal"),
    CharLiteral(char).label("character literal"),
    StrLiteral(string).label("string literal"),
    Identifier(ident),
    openP ~> expr <~ closeP
  )(
    Ops(Prefix)(
      Not <# "!",
      notFollowedBy(integer) ~> (Neg <# "-".label("unary operator"))
    ),
    Ops(InfixL)(Mul <# "*", Mod <# "%", Div <# "/"),
    Ops(InfixL)(Add <# "+", Sub <# "-".label("binary operator")),
    Ops(InfixN)(
      Greater <# ">",
      GreaterEq <# ">=",
      Less <# "<",
      LessEq <# "<="
    ),
    Ops(InfixN)(Eq <# "==", Neq <# "!="),
    Ops(InfixR)(And <# "&&"),
    Ops(InfixR)(Or <# "||")
  )
    .label("expression")
    .explain(
      "expressions may start with integer, string, character or boolean literals; identifiers; unary operators; null; or parentheses"
    )
    .explain(
      "in addition, expressions may contain array indexing operations; and comparison, logical, and arithmetic operators"
    )

private lazy val ident = Ident(identifier)
private lazy val openP = "("
private lazy val closeP = ")"
