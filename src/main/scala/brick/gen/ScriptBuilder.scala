package brick.gen

import brick.link.PacmanHelper
import brick.util.IndentedStringBuilder
import brick.tmpl.BrickTmplProc

abstract class ScriptBuilder {
  val ext: String
  val threads: String
  val unix: Boolean

  val pacman: Option[String] = PacmanHelper.getPackageManager

  def comment(text: String)(implicit b: IndentedStringBuilder): Unit

  def set(
      variable: String,
      value: String
  )(implicit b: IndentedStringBuilder): Unit

  def newline()(implicit b: IndentedStringBuilder): Unit = {
    b ++= "\n"
  }

  def function(
      name: String
  )(body: => Unit)(implicit b: IndentedStringBuilder): Unit

  def iffail(body: => Unit)(implicit b: IndentedStringBuilder): Unit

  def ifnexists(path: String)(body: => Unit)(implicit
      b: IndentedStringBuilder
  ): Unit

  def call(
      name: String,
      args: String*
  )(implicit b: IndentedStringBuilder): Unit

  def rawTemplated(
      text: String
  )(implicit b: IndentedStringBuilder): Unit = {
    raw(BrickTmplProc.processTemplate(text))(b)
  }

  def raw(
      text: String
  )(implicit b: IndentedStringBuilder): Unit = {
    val heredocPattern = """.*<<\s*(\w+)\s*$""".r
    text match {
      case heredocPattern(eofMarker) =>
        b ++= s"$text\n"
        setHeredocMode(eofMarker, b)
      case _ if isInHeredocMode(text) =>
        if (isHeredocEnd(text)) {
          b.appendRaw(s"$text\n")
          exitHeredocMode(b)
        } else {
          b.appendRaw(s"$text\n")
        }
      case _ =>
        // Normal text, append with indentation
        b ++= s"$text\n"
    }
  }

  private var heredocMarker: Option[String] = None
  private var savedIndent: String = ""

  private def setHeredocMode(marker: String, b: IndentedStringBuilder): Unit = {
    heredocMarker = Some(marker)
    savedIndent = b.getCurrentIndent
    b.setIndent("")
  }

  private def isInHeredocMode(text: String): Boolean = heredocMarker.isDefined

  private def isHeredocEnd(text: String): Boolean = {
    heredocMarker.contains(text.trim)
  }

  private def exitHeredocMode(b: IndentedStringBuilder): Unit = {
    // Restore the saved indent level
    b.setIndent(savedIndent)
    heredocMarker = None
    savedIndent = ""
  }

  def out()(implicit b: IndentedStringBuilder): String =
    b.mkString

  def install(pkg: String)(implicit b: IndentedStringBuilder): Unit =
    pacman.foreach { b ++= _ + s" install $pkg\n" }

  def input(name: String)(implicit b: IndentedStringBuilder): Unit
}

class BashScriptBuilder extends ScriptBuilder {
  override val ext: String = ".sh"
  override val threads: String = "$(nproc)"
  override val unix: Boolean = true

  override def comment(text: String)(implicit b: IndentedStringBuilder): Unit =
    b ++= s"# $text\n"

  override def set(variable: String, value: String)(implicit
      b: IndentedStringBuilder
  ): Unit =
    b ++= s"export $variable=\"$value\"\n"

  override def function(
      name: String
  )(body: => Unit)(implicit b: IndentedStringBuilder): Unit = {
    b ++= s"$name() {\n"
    b.indent()
    body
    b.outdent()
    b ++= "}\n\n"
  }

  override def iffail(body: => Unit)(implicit b: IndentedStringBuilder): Unit =
    b ++= s"if [ ! $$? -eq 0 ]; then\n"
    b.indent()
    body
    b.outdent()
    b ++= "fi\n"

  override def ifnexists(path: String)(body: => Unit)(implicit
      b: IndentedStringBuilder
  ): Unit =
    b ++= s"if [ ! -e \"$path\" ]; then\n"
    b.indent()
    body
    b.outdent()
    b ++= "fi\n"

  override def call(name: String, args: String*)(implicit
      b: IndentedStringBuilder
  ): Unit = args match {
    case Nil => b ++= s"$name\n"
    case _   => b ++= s"$name ${args.mkString(" ")}\n"
  }

  override def out()(implicit b: IndentedStringBuilder): String =
    s"#!/bin/bash\n${b.mkString}"

  override def input(name: String)(implicit b: IndentedStringBuilder): Unit =
    b ++= s". ./$name$ext\n"
}

class PowerShellScriptBuilder extends ScriptBuilder {
  override val ext: String = ".ps1"
  override val threads: String =
    "$(Get-CimInstance -ClassName Win32_Processor | Select-Object -ExpandProperty NumberOfLogicalProcessors)"
  override val unix: Boolean = false

  override def comment(text: String)(implicit b: IndentedStringBuilder): Unit =
    b ++= s"# $text\n"

  override def set(variable: String, value: String)(implicit
      b: IndentedStringBuilder
  ): Unit =
    b ++= s"$$Env:$variable = \"$value\"\n"

  override def function(
      name: String
  )(body: => Unit)(implicit b: IndentedStringBuilder): Unit = {
    b ++= s"function $name {\n"
    b.indent()
    body
    b.outdent()
    b ++= "}\n\n"
  }

  override def iffail(body: => Unit)(implicit b: IndentedStringBuilder): Unit =
    b ++= s"if ($$? -ne 0) {\n"
    b.indent()
    body
    b.outdent()
    b ++= "}\n"

  override def ifnexists(path: String)(body: => Unit)(implicit
      b: IndentedStringBuilder
  ): Unit =
    b ++= s"if (-not (Test-Path -Path \"$path\")) {\n"
    b.indent()
    body
    b.outdent()
    b ++= "}\n"

  override def call(name: String, args: String*)(implicit
      b: IndentedStringBuilder
  ): Unit =
    args match {
      case Nil => b ++= s"$name\n"
      case _   => b ++= s"$name \"${args.mkString("\" \"")}\"\n"
    }

  override def input(name: String)(implicit b: IndentedStringBuilder): Unit =
    b ++= s". ./$name$ext\n"
}
