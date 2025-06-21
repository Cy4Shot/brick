package brick.gen

import java.io.{File, PrintWriter}
import brick.log.LoggingCtx
import brick.util.Platform

abstract class Generator {
  def generate()(implicit builder: ScriptBuilder): String

  def validate()(implicit builder: ScriptBuilder): Unit

  def generateToFile(filePath: String): Unit = {
    given builder: ScriptBuilder = Platform.isWindows match {
      case true  => new PowerShellScriptBuilder()
      case false => new BashScriptBuilder()
    }

    validate()
    val content = generate()
    val writer = new PrintWriter(new File(filePath + builder.ext))
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }
}
