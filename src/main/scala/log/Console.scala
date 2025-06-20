package brick.log

import fansi._
import scala.concurrent.duration._
import scala.util.Random
import scala.Console.{print => cprint}

import scala.sys.process._
import scala.util.Try

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
  private val topLeft = "╭"
  private val topRight = "╮"
  private val bottomLeft = "╰"
  private val bottomRight = "╯"
  private val horizontal = "─"
  private val vertical = "│"

  def getTerminalSize(): Option[(Int, Int)] = {
    Try {
      val cols = "tput cols".!!.trim.toInt
      val rows = "tput lines".!!.trim.toInt
      (rows, cols)
    }.toOption
  }

  private def getVisibleLength(str: String): Int = {
    str.replaceAll("\u001B\\[[;\\d]*m", "").length
  }

  private def padToWidth(content: String, targetWidth: Int): String = {
    val visibleLength = getVisibleLength(content)
    val paddingNeeded = math.max(0, targetWidth - visibleLength)
    content + " " * paddingNeeded
  }

  def render(): (String, Int) = {
    System.setProperty("org.jline.utils.Log", "OFF")
    val terminalWidth = getTerminalSize() match {
      case Some((_, width)) => width
      case None =>
        80 // Fallback to a default width if terminal size cannot be determined
    }
    val boxWidth = terminalWidth - 2
    val innerWidth = boxWidth - 2

    val truncatedTitle =
      if (title.length > innerWidth - 1) title.take(innerWidth - 4) + "..."
      else title

    val logLines = logs.takeRight(maxLogLines).map { log =>
      if (getVisibleLength(log) > innerWidth) {
        log.take(innerWidth - 3) + "..."
      } else log
    }

    val contentLines = bars.takeRight(maxLines).map { bar =>
      val rendered = bar.render(maxWidth = innerWidth)
      if (getVisibleLength(rendered) > innerWidth) {
        val truncated = rendered.take(innerWidth - 3) + "..."
        if (
          truncated.contains("\u001B") && !truncated
            .matches(".*\u001B\\[[;\\d]*m")
        ) {
          rendered.take(innerWidth)
        } else truncated
      } else rendered
    }

    val top = s"$topLeft${horizontal * (boxWidth - 2)}$topRight"
    val titleContent =
      Str(truncatedTitle).overlay(Bold.On, 0, truncatedTitle.length).render
    val titleLine =
      s"$vertical ${padToWidth(titleContent, innerWidth - 1)}$vertical"
    val midSep = s"$vertical${horizontal * (boxWidth - 2)}$vertical"

    val logBodyLines = logLines.map { line =>
      s"$vertical${padToWidth(line, innerWidth)}$vertical"
    }

    val logSep =
      if (logLines.nonEmpty)
        Seq(s"$vertical${horizontal * (boxWidth - 2)}$vertical")
      else Seq.empty

    val progressBodyLines = contentLines.map { line =>
      s"$vertical${padToWidth(line, innerWidth)}$vertical"
    }

    val bottom = s"$bottomLeft${horizontal * (boxWidth - 2)}$bottomRight"

    val allLines =
      top +: titleLine +: midSep +: (logBodyLines ++ logSep ++ progressBodyLines) :+ bottom
    (allLines.mkString("\n"), allLines.length)
  }

  def lineCount: Int = {
    val logSepCount = if (logs.nonEmpty) 1 else 0
    3 + math.min(logs.length, maxLogLines) + logSepCount + math.min(
      bars.length,
      maxLines
    )
  }
}
