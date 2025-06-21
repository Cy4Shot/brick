package brick.gen

import scala.collection.mutable.StringBuilder

abstract class ScriptBuilder {
  val ext: String
  val threads: String

  def comment(text: String)(implicit b: StringBuilder): Unit

  def set(
      variable: String,
      value: String
  )(implicit b: StringBuilder): Unit

  def newline()(implicit b: StringBuilder): Unit = {
    b ++= "\n"
  }

  def out()(implicit b: StringBuilder): String =
    b.mkString
}

class BashScriptBuilder extends ScriptBuilder {
  override val ext: String = ".sh"
  override val threads: String = "$(nproc)"

  override def comment(text: String)(implicit b: StringBuilder): Unit =
    b ++= s"# $text\n"

  override def set(variable: String, value: String)(implicit b: StringBuilder): Unit =
    b ++= s"$variable=\"$value\"\n"
}

class PowerShellScriptBuilder extends ScriptBuilder {
  override val ext: String = ".ps1"
  override val threads: String = "$(Get-CimInstance -ClassName Win32_Processor | Select-Object -ExpandProperty NumberOfLogicalProcessors)"

  override def comment(text: String)(implicit b: StringBuilder): Unit =
    b ++= s"# $text\n"

  override def set(variable: String, value: String)(implicit b: StringBuilder): Unit =
    b ++= s"$$Env:$variable = \"$value\"\n"
}