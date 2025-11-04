package brick.tmpl

import scala.util.matching.Regex

import parsley.Success
import parsley.Failure

import brick.parse.tmpl._
import brick.parse.tmpl.Expr._
import brick.conc.BricksCtx

object BrickTmplProc {

  val templatePattern: Regex = """\{\{\s*(.*?)\s*\}\}""".r

  def processTemplate(input: String)(using BricksCtx): String = {
    val sb = new StringBuilder
    var lastIndex = 0
    for (m <- templatePattern.findAllMatchIn(input)) {
      sb.append(input.substring(lastIndex, m.start))
      sb.append(parse(m.group(1)))
      lastIndex = m.end
    }
    sb.append(input.substring(lastIndex))
    sb.toString()
  }

  private def parse(rawExpr: String)(using BricksCtx): String = {
    val stmt = parseExpr(rawExpr) match {
      case Success(x)   => x
      case Failure(msg) => throw new IllegalArgumentException(msg)
    }
    given SymbolTable = builtin.flatten()
    typecheck(stmt)
    val evalled = eval(stmt).toString()
    // println(s"Parsed expression: ${stmt} => Evaluated to: $evalled")
    evalled
  }

  private def resolveIdentifier(
      name: String,
      symbols: SymbolTable,
      ctx: BricksCtx
  ): (BricksCtx => Any, brick.parse.tmpl.Type.Type) = {
    symbols.get(name) match {
      case Some(Left((handler, ty))) => (handler, ty)
      case Some(Right(_)) =>
        throw new RuntimeException(s"Unexpected wildcard entry for: $name")
      case None =>
        val wildcardMatch = symbols.iterator
          .find {
            case (pattern, Right(_)) if pattern.endsWith(".*") =>
              val prefix = pattern.stripSuffix(".*")
              name.startsWith(prefix + ".")
            case _ => false
          }
          .collect { case (pattern, Right((wildcardHandler, ty))) =>
            val prefix = pattern.stripSuffix(".*")
            val captured = name.stripPrefix(prefix + ".")
            val handler: BricksCtx => Any = wildcardHandler(captured)
            (handler, ty)
          }

        wildcardMatch.getOrElse(
          throw new RuntimeException(s"Undefined variable: $name")
        )
    }
  }

  def eval(stmt: TmplStmt)(using SymbolTable)(using BricksCtx): Any =
    stmt match {
      case IfElse(cond, thenBranch, elseBranch) =>
        if (eval(cond).asInstanceOf[Boolean]) {
          eval(thenBranch)
        } else {
          eval(elseBranch)
        }
      case expr: Expr => eval(expr)
    }

  def eval(expr: Expr)(using symbols: SymbolTable)(using ctx: BricksCtx): Any =
    expr match {
      // ---------- Literals ----------
      case IntLiteral(x)  => x
      case BoolLiteral(b) => b
      case CharLiteral(c) => c
      case StrLiteral(v)  => v

      // ---------- Identifiers ----------
      case Identifier(Ident(name)) =>
        val (handler, _) = resolveIdentifier(name, symbols, ctx)
        handler.apply(ctx)

      // ---------- Unary ----------
      case Not(e) => !eval(e).asInstanceOf[Boolean]
      case Neg(e) => -eval(e).asInstanceOf[Int]

      // ---------- Arithmetic ----------
      case Add(l, r) => eval(l).asInstanceOf[Int] + eval(r).asInstanceOf[Int]
      case Sub(l, r) => eval(l).asInstanceOf[Int] - eval(r).asInstanceOf[Int]
      case Mul(l, r) => eval(l).asInstanceOf[Int] * eval(r).asInstanceOf[Int]
      case Div(l, r) => eval(l).asInstanceOf[Int] / eval(r).asInstanceOf[Int]
      case Mod(l, r) => eval(l).asInstanceOf[Int] % eval(r).asInstanceOf[Int]

      // ---------- Comparison ----------
      case Greater(l, r) =>
        eval(l).asInstanceOf[Int] > eval(r).asInstanceOf[Int]
      case GreaterEq(l, r) =>
        eval(l).asInstanceOf[Int] >= eval(r).asInstanceOf[Int]
      case Less(l, r) => eval(l).asInstanceOf[Int] < eval(r).asInstanceOf[Int]
      case LessEq(l, r) =>
        eval(l).asInstanceOf[Int] <= eval(r).asInstanceOf[Int]

      // ---------- Equality ----------
      case Eq(l, r)  => eval(l) == eval(r)
      case Neq(l, r) => eval(l) != eval(r)

      // ---------- Logical ----------
      case And(l, r) =>
        eval(l).asInstanceOf[Boolean] && eval(r).asInstanceOf[Boolean]
      case Or(l, r) =>
        eval(l).asInstanceOf[Boolean] || eval(r).asInstanceOf[Boolean]

      case _ => throw new RuntimeException(s"Unhandled expression: $expr")
    }
}
