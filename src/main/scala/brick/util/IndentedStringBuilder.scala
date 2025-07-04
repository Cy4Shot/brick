package brick.util

import scala.annotation.targetName
import scala.collection.mutable

class IndentedStringBuilder(indentStep: String = "  ") {
  private val sb = new mutable.StringBuilder
  private var currentIndent = ""
  private var isNewLine = true

  def indent(): Unit = {
    currentIndent += indentStep
  }

  def outdent(): Unit = {
    if (currentIndent.length >= indentStep.length)
      currentIndent = currentIndent.substring(0, currentIndent.length - indentStep.length)
  }

  def getCurrentIndent: String = currentIndent

  def setIndent(indent: String): Unit = {
    currentIndent = indent
  }

  def appendRaw(text: String): this.type = {
    sb.append(text)
    this
  }

  def append(text: String): this.type = {
    if (text.isEmpty) return this
    
    val lines = text.split("\n", -1)
    for (i <- lines.indices) {
      val line = lines(i)
      val isLastLine = i == lines.length - 1
      
      if (isNewLine && line.nonEmpty) {
        sb.append(currentIndent)
      }
      sb.append(line)
      
      if (!isLastLine) {
        sb.append("\n")
        isNewLine = true
      } else {
        isNewLine = text.endsWith("\n")
      }
    }
    this
  }

  @targetName("appendText")
  def ++=(text: String): this.type = append(text)

  def mkString: String = sb.mkString
}
