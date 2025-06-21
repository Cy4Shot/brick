package brick.gen.impl

import brick.gen._
import brick.log.LoggingCtx
import brick.conc.Brick
import brick.parse.BrickAST.{GitSource, UrlSource, GithubSource}
import brick.util.IndentedStringBuilder

class BricksGenerator(val brick: Brick)(implicit ctx: LoggingCtx)
    extends Generator {

  val name: String = brick.name
  val NAME: String = brick.name.toUpperCase

  def validate()(implicit builder: ScriptBuilder): Unit = {}

  def generate()(implicit builder: ScriptBuilder): String = {
    given config: IndentedStringBuilder = IndentedStringBuilder()

    builder.comment(s"Welcome to the builder for ${brick.name}")
    builder.newline()

    builder.set(s"${NAME}_BUILD_DIR", s"$${BUILD_DIR}/$name")
    builder.set(s"${NAME}_INSTALL_DIR", s"$${INSTALL_DIR}/$name")

    brick.source.source match {
      case GitSource(_) => {
        builder.set(s"${NAME}_URL", brick.source.loc(0))
        builder.set(s"${NAME}_HASH", brick.source.loc(1))
      }
      case UrlSource(pos) => {
        builder.set(s"${NAME}_URL", brick.source.loc(0))
      }
      case GithubSource(_) => {
        builder.set(
          s"${NAME}_URL",
          s"https://github.com/${brick.source.loc(0)}.git"
        )
        builder.set(s"${NAME}_HASH", brick.source.loc(1))
      }
    }
    builder.newline()

    builder.comment("Sets the environment variables for compilation!")
    builder.function(name + "_conf") {
      builder.set("CFLAGS", "GLOBAL_CFLAGS")
      builder.set("CXXFLAGS", "GLOBAL_CXXFLAGS")
      builder.set("FCFLAGS", "GLOBAL_FCFLAGS")
    }

    builder.comment("Downloads the source code and prepares it for compilation")
    builder.function(name + "_get") {
      brick.source.source match {
        case UrlSource(pos) => {
          builder.call("curldownload", s"$$${NAME}_BUILD_DIR", s"$$${NAME}_URL")
        }
        case GitSource(_) | GithubSource(_) => {
          builder.call("gitdownload", s"$$${NAME}_BUILD_DIR", s"$$${NAME}_URL", s"$$${NAME}_HASH")
        }
      }
      builder.iffail {
        builder.call("errecho", s"Failed to download ${brick.name} source!")
      }
    }

    builder.comment("Compiles the source code")
    builder.function(name + "_build") {
      builder.call("pushd", s"$$${NAME}_BUILD_DIR")
      builder.call(name + "_conf")

      brick.commands.map(builder.raw)

      builder.call("popd")
      builder.call("touch", s"$$${NAME}_ROOT_DIR/${name}.flag")
    }

    builder.comment("Sets up the env variables for the next build")
    builder.function(name + "_env") {
      builder.set("PATH", s"$${${NAME}_INSTALL_DIR}/bin:$$PATH")
      builder.set("CPATH", s"$${${NAME}_INSTALL_DIR}/include:$$CPATH")
      builder.set("CMAKE_PREFIX_PATH", s"$${${NAME}_INSTALL_DIR}:$$CMAKE_PREFIX_PATH")
      builder.set("LIBRARY_PATH", s"$${${NAME}_INSTALL_DIR}/lib:$$LIBRARY_PATH")
      builder.set("LD_LIBRARY_PATH", s"$${${NAME}_INSTALL_DIR}/lib:$$LD_LIBRARY_PATH")
    }

    builder.comment(s"Create a full build of $name")
    builder.function(name + "_full") {
      builder.ifnexists(s"$$${NAME}_ROOT_DIR/${name}.flag") {
        builder.call(name + "_get")
        builder.call(name + "_build")
      }
      builder.call(name + "_env")
    }

    builder.out()
  }
}
