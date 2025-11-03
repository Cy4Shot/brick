package brick.parse

import brick.parse.Bridges.*
import parsley.generic.*

object BrickAST {

  protected sealed trait ASTLeaf {
    def prettyPrint: String

    val pos: Pos = (0, 0)
  }

  sealed trait Source extends ASTLeaf
  sealed trait Stmt extends ASTLeaf
  sealed trait Flag extends Stmt
  sealed trait FlagOpt extends ASTLeaf

  case class ModulesFlag(modules: List[ModuleOpt])(override val pos: Pos)
      extends Flag {
    override def prettyPrint: String =
      s"modules ${modules.map(_.prettyPrint).mkString(" ")}"
  }

  case class EnvironmentFlag(envs: EnvOpt)(override val pos: Pos) extends Flag {
    override def prettyPrint: String = "env " + envs.prettyPrint
  }

  case class SourceFlag(sources: SourceOpt)(override val pos: Pos)
      extends Flag {
    override def prettyPrint: String = "source " + sources.prettyPrint
  }

  case class TargetFlag(target: BasicOpt)(override val pos: Pos) extends Flag {
    override def prettyPrint: String = "target " + target.prettyPrint
  }

  case class DependenciesFlag(dependency: BasicOpt)(override val pos: Pos)
      extends Flag {
    override def prettyPrint: String = "dep " + dependency.prettyPrint
  }

  case class PackageFlag(pkg: BasicOpt)(override val pos: Pos) extends Flag {
    override def prettyPrint: String = "pkg " + pkg.prettyPrint
  }

  case class CompilerFlag(compiler: Compiler.Compiler, binary: BasicOpt)(
      override val pos: Pos
  ) extends Flag {
    override def prettyPrint: String =
      compiler.prettyPrint + " " + binary.prettyPrint
  }

  case class CompilerFlagsFlag(
      compiler: CompilerFlags.CompilerFlags,
      flag: List[BasicOpt]
  )(
      override val pos: Pos
  ) extends Flag {
    override def prettyPrint: String =
      compiler.prettyPrint + " " + flag.map(_.prettyPrint).mkString(" ")
  }

  case class CommandStmt(command: String)(override val pos: Pos) extends Stmt {
    override def prettyPrint: String = command
  }

  case class BasicOpt(opt: String) extends FlagOpt {
    override def prettyPrint: String = opt
  }

  case class EnvOpt(name: String, value: String) extends FlagOpt {
    override def prettyPrint: String = s"$name:$value"
  }

  case class ModuleOpt(name: String, version: Option[String]) extends FlagOpt {
    override def prettyPrint: String = version match {
      case Some(v) => s"$name/$v"
      case None    => name
    }
  }

  case class GitSource(override val pos: Pos) extends Source {
    override def prettyPrint: String = "git"
  }

  case class UrlSource(override val pos: Pos) extends Source {
    override def prettyPrint: String = "url"
  }

  case class GithubSource(override val pos: Pos) extends Source {
    override def prettyPrint: String = "gh"
  }

  case class SourceOpt(source: Source, loc: List[String]) extends FlagOpt {
    override def prettyPrint: String =
      s"${source.prettyPrint}:${loc.mkString(":")}"
  }

  case class Program(stmts: List[Stmt]) extends ASTLeaf {
    override def prettyPrint: String =
      stmts.map(_.prettyPrint).mkString("\n")
  }

  object Compiler {
    sealed trait Compiler extends ASTLeaf {
      override def prettyPrint: String = this match {
        case Compiler.CC(_)     => "cc"
        case Compiler.CXX(_)    => "cxx"
        case Compiler.FC(_)     => "fc"
        case Compiler.MPICC(_)  => "mpicc"
        case Compiler.MPICXX(_) => "mpicxx"
        case Compiler.MPIFC(_)  => "mpifc"
      }
    }

    case class CC(override val pos: Pos) extends Compiler
    case class CXX(override val pos: Pos) extends Compiler
    case class FC(override val pos: Pos) extends Compiler
    case class MPICC(override val pos: Pos) extends Compiler
    case class MPICXX(override val pos: Pos) extends Compiler
    case class MPIFC(override val pos: Pos) extends Compiler

    object CC extends ParserBridgePos0[CC]
    object CXX extends ParserBridgePos0[CXX]
    object FC extends ParserBridgePos0[FC]
    object MPICC extends ParserBridgePos0[MPICC]
    object MPICXX extends ParserBridgePos0[MPICXX]
    object MPIFC extends ParserBridgePos0[MPIFC]
  }

  object CompilerFlags {
    sealed trait CompilerFlags extends ASTLeaf {
      override def prettyPrint: String = this match {
        case CompilerFlags.CFLAGS(_)   => "cflags"
        case CompilerFlags.CXXFLAGS(_) => "cxxflags"
        case CompilerFlags.FCFLAGS(_)  => "fcflags"
      }
    }

    case class CFLAGS(override val pos: Pos) extends CompilerFlags
    case class CXXFLAGS(override val pos: Pos) extends CompilerFlags
    case class FCFLAGS(override val pos: Pos) extends CompilerFlags

    object CFLAGS extends ParserBridgePos0[CFLAGS]
    object CXXFLAGS extends ParserBridgePos0[CXXFLAGS]
    object FCFLAGS extends ParserBridgePos0[FCFLAGS]
  }

  object ModulesFlag extends ParserBridgePos1[List[ModuleOpt], ModulesFlag]
  object EnvironmentFlag extends ParserBridgePos1[EnvOpt, EnvironmentFlag]
  object SourceFlag extends ParserBridgePos1[SourceOpt, SourceFlag]
  object TargetFlag extends ParserBridgePos1[BasicOpt, TargetFlag]
  object DependenciesFlag extends ParserBridgePos1[BasicOpt, DependenciesFlag]
  object PackageFlag extends ParserBridgePos1[BasicOpt, PackageFlag]
  object CompilerFlag
      extends ParserBridgePos2[Compiler.Compiler, BasicOpt, CompilerFlag]
  object CompilerFlagsFlag
      extends ParserBridgePos2[CompilerFlags.CompilerFlags, List[
        BasicOpt
      ], CompilerFlagsFlag]

  object GitSource extends ParserBridgePos0[GitSource]
  object UrlSource extends ParserBridgePos0[UrlSource]
  object GithubSource extends ParserBridgePos0[GithubSource]

  object BasicOpt extends ParserBridge1[String, BasicOpt]
  object EnvOpt extends ParserBridge2[String, String, EnvOpt]
  object SourceOpt extends ParserBridge2[Source, List[String], SourceOpt]
  object ModuleOpt extends ParserBridge2[String, Option[String], ModuleOpt]

  object CommandStmt extends ParserBridgePos1[String, CommandStmt]
  object Program extends ParserBridge1[List[Stmt], Program]
}
