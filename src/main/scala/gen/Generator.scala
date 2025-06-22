package brick.gen

import java.io.{File, PrintWriter}
import brick.log.LoggingCtx
import brick.util.Platform

abstract class Generator {
  def generate()(implicit builder: ScriptBuilder): String

  def validate()(implicit builder: ScriptBuilder): Unit

  def generateToFile(filePath: String): Unit = {
    given builder: ScriptBuilder = Platform.isWindows match {
      case true  => new BashScriptBuilder()
      case false => new BashScriptBuilder()
    }

    validate()
    val content = generate()
    val file = new File("generated/" + filePath + builder.ext)
    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }
}
