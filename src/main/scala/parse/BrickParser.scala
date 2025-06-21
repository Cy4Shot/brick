package brick.parse

import brick.parse.BrickLexer._
import brick.parse.BrickLexer.implicits.implicitSymbol
import brick.parse.BrickAST._

import parsley.Parsley
import parsley.quick._
import parsley.errors.combinator._

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
    many(satisfy(c => c.toInt >= 33 && c.toInt < 127 && !c.isWhitespace && c != ':')).map(_.mkString)
}
