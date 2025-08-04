package brick.gen.impl

import brick.conc.Brick
import brick.gen.*
import brick.parse.BrickAST.{GitSource, GithubSource, UrlSource}
import brick.util.IndentedStringBuilder

class BricksGenerator(val brick: Brick) extends Generator {

  val name: String = brick.name.toLowerCase
  private val NAME: String = brick.name.toUpperCase

  def validate()(implicit builder: ScriptBuilder): Unit = {}

  def generate()(implicit builder: ScriptBuilder): String = {
    given config: IndentedStringBuilder = IndentedStringBuilder()

    builder.comment(s"Welcome to the builder for ${brick.name}")
    builder.newline()

    builder.set(s"${NAME}_BUILD_DIR", s"$${BUILD_DIR}/$name")
    builder.set(s"${NAME}_INSTALL_DIR", s"$${INSTALL_DIR}/$name")

    brick.source.source match {
      case GitSource(_) =>
        builder.set(s"${NAME}_URL", brick.source.loc.head)
        builder.set(s"${NAME}_HASH", brick.source.loc(1))
      case UrlSource(pos) =>
        builder.set(s"${NAME}_URL", brick.source.loc.mkString(":"))
      case GithubSource(_) =>
        builder.set(
          s"${NAME}_URL",
          s"https://github.com/${brick.source.loc.head}.git"
        )
        builder.set(s"${NAME}_HASH", brick.source.loc(1))
    }
    builder.newline()

    builder.comment("Sets the environment variables for compilation!")
    builder.function(name + "_conf") {
      builder.set("CFLAGS", "$GLOBAL_CFLAGS")
      builder.set("CXXFLAGS", "$GLOBAL_CXXFLAGS")
      builder.set("FCFLAGS", "$GLOBAL_FCFLAGS")
    }

    builder.comment("Downloads the source code and prepares it for compilation")
    builder.function(name + "_get") {
      brick.source.source match {
        case UrlSource(pos) =>
          builder.call("curldownload", s"$$${NAME}_BUILD_DIR", s"$$${NAME}_URL")
        case GitSource(_) | GithubSource(_) =>
          builder.call(
            "gitdownload",
            s"$$${NAME}_BUILD_DIR",
            s"$$${NAME}_URL",
            s"$$${NAME}_HASH"
          )
      }
      builder.iffail {
        builder.call("errecho", s"Failed to download ${brick.name} source!")
      }
    }

    builder.comment("Compiles the source code")
    builder.function(name + "_build") {
      builder.call("pushd", s"$$${NAME}_BUILD_DIR")
      builder.call(name + "_conf")

      brick.commands.foreach(builder.rawTemplated)

      builder.call("popd")
      builder.call("touch", s"$$TMP_DIR/$name.flag")
    }

    builder.comment("Sets up the env variables for the next build")
    builder.function(name + "_env") {
      builder.set("PATH", s"$${${NAME}_INSTALL_DIR}/bin:$${PATH:-}")
      builder.set("CPATH", s"$${${NAME}_INSTALL_DIR}/include:$${CPATH:-}")
      builder.set(
        "CMAKE_PREFIX_PATH",
        s"$${${NAME}_INSTALL_DIR}:$${CMAKE_PREFIX_PATH:-}"
      )
      builder.set(
        "LIBRARY_PATH",
        s"$${${NAME}_INSTALL_DIR}/lib:$${LIBRARY_PATH:-}"
      )
       builder.set(
        "LIBRARY_PATH",
        s"$${${NAME}_INSTALL_DIR}/lib64:$${LIBRARY_PATH:-}"
      )
      builder.set(
        "LD_LIBRARY_PATH",
        s"$${${NAME}_INSTALL_DIR}/lib:$${LD_LIBRARY_PATH:-}"
      )
      builder.set(
        "LD_LIBRARY_PATH",
        s"$${${NAME}_INSTALL_DIR}/lib64:$${LD_LIBRARY_PATH:-}"
      )
    }

    builder.comment(s"Create a full build of $name")
    builder.function(name + "_full") {
      builder.call("taskecho", name)
      builder.ifnexists(s"$$TMP_DIR/$name.flag") {
        builder.call("mkdir", "-p", s"$$TMP_DIR/$name")
        builder.call("stepecho", s"Downloading $name...")
        builder.call(name + "_get", s"> \"$$TMP_DIR/$name/download.log\" 2> >(tee -a \"$$TMP_DIR/$name/download.log\" >&2)")
        builder.call("stepecho", s"Compiling $name...")
        builder.call(name + "_build", s"> \"$$TMP_DIR/$name/build.log\" 2> >(tee -a \"$$TMP_DIR/$name/build.log\" >&2)")
      }
      builder.call("successecho", s"Built $name!")
      builder.call(name + "_env")
    }

    builder.out()
  }
}
