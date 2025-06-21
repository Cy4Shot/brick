package brick.gen

import scala.collection.mutable.StringBuilder

abstract class ScriptBuilder {
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
  override def comment(text: String)(implicit b: StringBuilder): Unit =
    b ++= s"# $text\n"

  override def set(variable: String, value: String)(implicit b: StringBuilder): Unit =
    b ++= s"$variable=\"$value\"\n"
}

class PowerShellScriptBuilder extends ScriptBuilder {
  override def comment(text: String)(implicit b: StringBuilder): Unit =
    b ++= s"# $text\n"

  override def set(variable: String, value: String)(implicit b: StringBuilder): Unit =
    b ++= s"$$Env:$variable = \"$value\"\n"
}