package brick.gen.impl

import brick.conc.Bricks
import brick.gen.*
import brick.link.ModuleSystem
import brick.parse.BrickAST.{GitSource, GithubSource, UrlSource}
import brick.util.IndentedStringBuilder

class MainGenerator(val bricks: Bricks) extends Generator {

  val name: String = bricks.name.toLowerCase

  def validate()(implicit builder: ScriptBuilder): Unit = {}

  def generate()(implicit builder: ScriptBuilder): String = {
    given config: IndentedStringBuilder = IndentedStringBuilder()

    builder.comment(s"Welcome to the compilation script for ${bricks.name}")
    builder.newline()

    // TODO: Make this cross platform
    builder.comment("Setting up the environment")
    builder.raw("set -euxo pipefail")
    if (ModuleSystem.hasModuleSystem) {
      builder.raw("module purge")
    }
    builder.newline()

    builder.comment("Loading required modules")
    builder.input("utils")
    builder.input("config")
    for (brick <- bricks.bricks) {
      builder.input(s"pkg/${brick.name.toLowerCase}")
    }
    builder.newline()

    builder.comment("Actually building the bricks")
    builder.call("titleecho", "Compiling bricks")

    for (brick <- bricks.bricks) {
      builder.call(s"${brick.name.toLowerCase}_full")
    }

    builder.out()
  }
}
