package brick.gen

import brick.util.Platform

import java.io.{File, PrintWriter}
import brick.util.FileUtil

abstract class Generator {
  def generate()(implicit builder: ScriptBuilder): String

  def validate()(implicit builder: ScriptBuilder): Unit

  def write(folder: FileUtil, name: String, executable: Boolean = false): Unit = {
    // TODO: Windows support
    given builder: ScriptBuilder = if Platform.isWindows then
      new BashScriptBuilder()
    else new BashScriptBuilder()

    val file = folder.sub(name + builder.ext);

    validate()
    file.write(generate(), executable = executable)
  }
}
