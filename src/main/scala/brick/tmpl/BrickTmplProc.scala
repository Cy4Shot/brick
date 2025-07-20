package brick.tmpl

import scala.util.matching.Regex

import parsley.Success
import parsley.Failure

import brick.parse.tmpl.parseExpr
import brick.parse.tmpl.Ident
import brick.parse.tmpl.Expr._

object BrickTmplProc {

  val templatePattern: Regex = """\{\{\s*(.*?)\s*\}\}""".r

  def processTemplate(input: String): String =
    templatePattern.replaceAllIn(input, m => parse(m.group(1)))

  private def parse(rawExpr: String): String = {
    val expr = parseExpr(rawExpr) match {
      case Success(x) => x
      case Failure(msg) => throw new IllegalArgumentException(msg)
    }
    val evalled = eval(expr, builtin.flatten()).toString()
    println(s"Parsed expression: ${expr} => Evaluated to: $evalled")
    evalled
  }

  def eval(expr: Expr, symbols: SymbolTable): Any = expr match {
     // ---------- Literals ----------
    case IntLiteral(x)    => x
    case BoolLiteral(b)   => b
    case CharLiteral(c)   => c
    case StrLiteral(v)    => v

    // ---------- Identifiers ----------
    case Identifier(Ident(name)) =>
      symbols.getOrElse(name, throw new RuntimeException(s"Undefined variable: $name"))

    // ---------- Unary ----------
    case Not(e) => !eval(e, symbols).asInstanceOf[Boolean]
    case Neg(e) => -eval(e, symbols).asInstanceOf[Int]

    // ---------- Arithmetic ----------
    case Add(l, r) => eval(l, symbols).asInstanceOf[Int] + eval(r, symbols).asInstanceOf[Int]
    case Sub(l, r) => eval(l, symbols).asInstanceOf[Int] - eval(r, symbols).asInstanceOf[Int]
    case Mul(l, r) => eval(l, symbols).asInstanceOf[Int] * eval(r, symbols).asInstanceOf[Int]
    case Div(l, r) => eval(l, symbols).asInstanceOf[Int] / eval(r, symbols).asInstanceOf[Int]
    case Mod(l, r) => eval(l, symbols).asInstanceOf[Int] % eval(r, symbols).asInstanceOf[Int]

    // ---------- Comparison ----------
    case Greater(l, r)   => eval(l, symbols).asInstanceOf[Int] > eval(r, symbols).asInstanceOf[Int]
    case GreaterEq(l, r) => eval(l, symbols).asInstanceOf[Int] >= eval(r, symbols).asInstanceOf[Int]
    case Less(l, r)      => eval(l, symbols).asInstanceOf[Int] < eval(r, symbols).asInstanceOf[Int]
    case LessEq(l, r)    => eval(l, symbols).asInstanceOf[Int] <= eval(r, symbols).asInstanceOf[Int]

    // ---------- Equality ----------
    case Eq(l, r)  => eval(l, symbols) == eval(r, symbols)
    case Neq(l, r) => eval(l, symbols) != eval(r, symbols)

    // ---------- Logical ----------
    case And(l, r) => eval(l, symbols).asInstanceOf[Boolean] && eval(r, symbols).asInstanceOf[Boolean]
    case Or(l, r)  => eval(l, symbols).asInstanceOf[Boolean] || eval(r, symbols).asInstanceOf[Boolean]

    case _ => throw new RuntimeException(s"Unhandled expression: $expr")
  }
}
