package brick.log

import fansi.*

import scala.Console.print as cprint
import scala.concurrent.duration.*
import scala.sys.process.*
import scala.util.{Random, Try}

case class ProgressBar(label: String, current: Long, max: Long) {
  def percent: Double =
    if (max == 0) 1.0 else (current.toDouble / max.toDouble).min(1.0).max(0.0)

  def color: Attr = percent match {
    case p if p < 0.3 => Color.Red
    case p if p < 0.7 => Color.Yellow
    case _            => Color.Green
  }

  def render(width: Int = 30, maxWidth: Int = Int.MaxValue): String = {
    val filled = (percent * width).toInt
    val empty = width - filled
    val bar = "█" * filled + " " * empty
    val barColored = Str(bar).overlay(color, 0, filled)

    val text = f"$label%-20s $current/$max (${Math.round(percent * 100)}%%) "
    val result = text + barColored.render
    if (result.length > maxWidth) result.take(maxWidth - 3) + "..." else result
  }
}

case class RichConsoleBoxWithLog(
    title: String,
    bars: Seq[ProgressBar],
    logs: Seq[String],
    maxLogLines: Int = 5,
    maxLines: Int = 10
) {
  private def getVisibleLength(str: String): Int = {
    str.replaceAll("\u001B\\[[;\\d]*m", "").length
  }

  def render(): (String, Int) = {
    System.setProperty("org.jline.utils.Log", "OFF")
    val terminalWidth = brick.link.TerminalSize.getTerminalWidth

    val truncatedTitle =
      if (title.length > terminalWidth - 1) title.take(terminalWidth - 4) + "..."
      else title

    val logLines = logs.takeRight(maxLogLines).map { log =>
      if (getVisibleLength(log) > terminalWidth) {
        log.take(terminalWidth - 3) + "..."
      } else log
    }

    val contentLines = bars.takeRight(maxLines).map { bar =>
      val rendered = bar.render(maxWidth = terminalWidth)
      if (getVisibleLength(rendered) > terminalWidth) {
        val truncated = rendered.take(terminalWidth - 3) + "..."
        if (
          truncated.contains("\u001B") && !truncated
            .matches(".*\u001B\\[[;\\d]*m")
        ) {
          rendered.take(terminalWidth)
        } else truncated
      } else rendered
    }

    val titleContent =
      Str(truncatedTitle).overlay(Bold.On, 0, truncatedTitle.length).render

    val allLines = titleContent +: (logLines ++ contentLines)
    (allLines.mkString("\n"), allLines.length)
  }

  def lineCount: Int = {
    1 + math.min(logs.length, maxLogLines) + math.min(bars.length, maxLines)
  }
}

def printError(msg: String): Unit = {
  println(fansi.Color.Red("Error: ") ++ fansi.Color.Yellow(msg))
}