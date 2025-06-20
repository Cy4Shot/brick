package brick.frontend

import brick.frontend.BrickLexer._
import brick.frontend.BrickLexer.implicits.implicitSymbol
import brick.AST._

import parsley.quick.{many, some, eof, sepBy, notFollowedBy, atomic, option, newline, spaces, endOfLine, manyTill, satisfy, stringOfMany}
import parsley.{Parsley, Result, Failure}
import parsley.expr.{precedence, Ops, Prefix, InfixL, InfixN, InfixR, chain}

import parsley.errors.combinator._
import parsley.errors.patterns.PreventativeErrors
import parsley.errors.VanillaGen
import parsley.errors.VanillaGen.UnexpectedItem
import parsley.errors.VanillaGen.NamedItem

object BrickParser {
  def parseString(input: String) =
    parser.parse(input)

  private lazy val parser = fully(program)

  private lazy val program: Parsley[Program] =
    Program(stmts)

  private lazy val stmts: Parsley[List[Stmt]] =
    many(many(endOfLine) ~> stmt)

  private lazy val stmt: Parsley[Stmt] =
    "@" ~> flagStmt | commandStmt

  private lazy val flagStmt: Parsley[Flag] =
    ModulesFlag("modules" ~> manyTill(moduleOpt, endOfLine))
      | EnvironmentFlag("env" ~> envOpt)
      | SourceFlag("source" ~> sourceOpt)
      | DependenciesFlag("dep" ~> basicOpt)

  private lazy val moduleOpt: Parsley[ModuleOpt] =
    ModuleOpt(identifier, option(version))

  private lazy val envOpt: Parsley[EnvOpt] =
    EnvOpt(identifier <~ ":" , path)

  private lazy val sourceOpt: Parsley[SourceOpt] =
    SourceOpt(source <~ ":", sepBy(path, ":"))

  private lazy val basicOpt: Parsley[BasicOpt] =
    BasicOpt(identifier)

  private lazy val source: Parsley[Source] =
    GitSource <# "git" |
      UrlSource <# "url" |
      GithubSource <# "gh"

  private lazy val commandStmt: Parsley[CommandStmt] =
    CommandStmt(manyTill(
      satisfy(c => c.toInt >= 32 && c != '\n' && c != '\r' && c != '\\') | ("\\" ~> endOfLine).as(' '),
    endOfLine).map(_.mkString.replaceAll("\\s{2,}", " ")))

  private lazy val version: Parsley[String] =
    "/" ~> identifier

  private lazy val path: Parsley[String] =
    many(satisfy(c => c.toInt >= 33 && c.toInt < 127 && !c.isWhitespace)).map(_.mkString)
}
