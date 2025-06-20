package brick

import parsley.generic._
import brick.frontend.Bridges._

object AST {

  protected sealed trait ASTLeaf {
    def prettyPrint: String

    val pos: Pos = (0, 0)
  }

  sealed trait Source extends ASTLeaf
  sealed trait Stmt extends ASTLeaf
  sealed trait Flag extends Stmt
  sealed trait FlagOpt extends ASTLeaf

  case class ModulesFlag(modules: List[ModuleOpt])(override val pos: Pos) extends Flag {
    override def prettyPrint: String = s"modules ${modules.map(_.prettyPrint).mkString(" ")}"
  }

  case class EnvironmentFlag(envs: EnvOpt)(override val pos: Pos) extends Flag {
    override def prettyPrint: String = "env " + envs.prettyPrint
  }

  case class SourceFlag(sources: SourceOpt)(override val pos: Pos) extends Flag {
    override def prettyPrint: String = "source " + sources.prettyPrint
  }

  case class DependenciesFlag(dependency: BasicOpt)(override val pos: Pos) extends Flag {
    override def prettyPrint: String = "dep " + dependency.prettyPrint
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
    override def prettyPrint: String = s"${source.prettyPrint}:${loc.mkString(":")}"
  }

  case class Program(stmts: List[Stmt]) extends ASTLeaf {
    override def prettyPrint: String =
      stmts.map(_.prettyPrint).mkString("\n")
  }


  object ModulesFlag extends ParserBridgePos1[List[ModuleOpt], ModulesFlag]
  object EnvironmentFlag extends ParserBridgePos1[EnvOpt, EnvironmentFlag]
  object SourceFlag extends ParserBridgePos1[SourceOpt, SourceFlag]
  object DependenciesFlag extends ParserBridgePos1[BasicOpt, DependenciesFlag]

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
