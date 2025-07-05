package brick.gen

import brick.log.LoggingCtx
import brick.util.Platform

import java.io.{File, PrintWriter}

abstract class Generator {
  def generate()(implicit builder: ScriptBuilder): String

  def validate()(implicit builder: ScriptBuilder): Unit

  def generateToFile(filePath: String, outputDir: String = "."): Unit = {
    // TODO: Windows support
    given builder: ScriptBuilder = if Platform.isWindows then
      new BashScriptBuilder()
    else new BashScriptBuilder()

    validate()
    val content = generate()
    val file = new File(s"$outputDir/$filePath${builder.ext}")
    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }
}
