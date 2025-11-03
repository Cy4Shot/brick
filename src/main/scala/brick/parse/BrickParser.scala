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

  def parseRootString(input: String): Result[String, Program] =
    rootParser.parse(input + "\n")

  private lazy val parser = fully(program)
  private lazy val rootParser = fully(rootProgram)

  private lazy val program: Parsley[Program] = Program(stmts)
  private lazy val rootProgram: Parsley[Program] = Program(rootStmts)

  private lazy val stmts: Parsley[List[Stmt]] =
    many("@" ~> flagStmt | commandStmt) <~ eof

  private lazy val rootStmts: Parsley[List[Stmt]] =
    many("@" ~> rootFlagStmt | commandStmt) <~ eof

  private lazy val flagStmt: Parsley[Flag] =
    ModulesFlag("modules" ~> manyTill(moduleOpt, endOfLine))
      | EnvironmentFlag("env" ~> envOpt)
      | SourceFlag("source" ~> sourceOpt)
      | DependenciesFlag("dep" ~> basicOpt)

  private lazy val rootFlagStmt: Parsley[Flag] = (
    TargetFlag("target" ~> basicOpt)
      | PackageFlag("pkg" ~> basicOpt)
      | CompilerFlagsFlag(
        CompilerFlags.CFLAGS <# atomic("cflags"),
        someTill(basicOpt, endOfLine)
      )
      | CompilerFlagsFlag(
        CompilerFlags.CXXFLAGS <# atomic("cxxflags"),
        someTill(basicOpt, endOfLine)
      )
      | CompilerFlagsFlag(
        CompilerFlags.FCFLAGS <# atomic("fcflags"),
        someTill(basicOpt, endOfLine)
      )
      | CompilerFlag(Compiler.CC <# atomic("cc"), basicOpt)
      | CompilerFlag(Compiler.CXX <# atomic("cxx"), basicOpt)
      | CompilerFlag(Compiler.FC <# atomic("fc"), basicOpt)
      | CompilerFlag(Compiler.MPICC <# atomic("mpicc"), basicOpt)
      | CompilerFlag(Compiler.MPICXX <# atomic("mpicxx"), basicOpt)
      | CompilerFlag(Compiler.MPIFC <# atomic("mpifc"), basicOpt)
  )

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
