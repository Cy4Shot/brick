package brick.util

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

  def ++=(text: String): this.type = append(text)

  def mkString: String = sb.mkString
}
