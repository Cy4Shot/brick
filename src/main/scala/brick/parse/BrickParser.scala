package brick.parse

import brick.parse.BrickAST.*
import brick.parse.BrickLexer.*
import brick.parse.BrickLexer.implicits.implicitSymbol
import parsley.errors.combinator.*
import parsley.quick.*
import parsley.{Parsley, Result}

object BrickParser {
  def parseString(input: String): Result[String, Program] =
    parser.parse(input + "\n")

  private lazy val parser = fully(program)

  private lazy val program: Parsley[Program] =
    Program(stmts)

  private lazy val stmts: Parsley[List[Stmt]] =
    many(stmt) <~ eof

  private lazy val stmt: Parsley[Stmt] =
    "@" ~> flagStmt | commandStmt

  private lazy val flagStmt: Parsley[Flag] =
    ModulesFlag("modules" ~> manyTill(moduleOpt, endOfLine))
      | TargetFlag("target" ~> basicOpt)
      | EnvironmentFlag("env" ~> envOpt)
      | SourceFlag("source" ~> sourceOpt)
      | DependenciesFlag("dep" ~> basicOpt)
      | PackageFlag("pkg" ~> basicOpt)

  private lazy val moduleOpt: Parsley[ModuleOpt] =
    ModuleOpt(identifier, option(version))

  private lazy val envOpt: Parsley[EnvOpt] =
    EnvOpt(identifier <~ ":", path)

  private lazy val sourceOpt: Parsley[SourceOpt] =
    SourceOpt(source <~ ":", sepBy(path, ":"))

  private lazy val basicOpt: Parsley[BasicOpt] =
    BasicOpt(identifier)

  private lazy val source: Parsley[Source] =
    GitSource <# "git" |
      UrlSource <# "url" |
      GithubSource <# "gh"

  private lazy val commandStmt: Parsley[CommandStmt] =
    CommandStmt(
      manyTill(
        atomic("\\" ~> endOfLine).as(' ') | satisfy(c =>
          c.toInt >= 32 && c != '\n' && c != '\r'
        ),
        endOfLine
      ).map(_.mkString.replaceAll("\\s{2,}", " "))
    )

  private lazy val version: Parsley[String] =
    "/" ~> identifier

  private lazy val path: Parsley[String] =
    many(
      satisfy(c =>
        c.toInt >= 33 && c.toInt < 127 && !c.isWhitespace && c != ':'
      )
    ).map(_.mkString)
}
