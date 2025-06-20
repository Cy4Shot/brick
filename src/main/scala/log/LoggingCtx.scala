package brick.log

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import java.util.concurrent.{Executors, ScheduledExecutorService}
import java.util.concurrent.atomic.AtomicBoolean
import scala.Console.{print => cprint}
import fansi.Str
import fansi.Color

class LoggingCtx(title: String, maxLogLines: Int = 12, maxProgressLines: Int = 5) {
  private var progressBars = mutable.LinkedHashMap.empty[String, ProgressBar]
  private var logs = mutable.Buffer.empty[String]
  private var lastLineCount = 0

  def addProgressBar(name: String, label: String, max: Long = 100): Unit = {
    progressBars(name) = ProgressBar(label, 0, max)
    render()
  }

  def updateProgress(name: String, current: Long): Unit = {
    progressBars.get(name).foreach { bar =>
      progressBars(name) = bar.copy(current = current)
    }
    render()
  }

  def setProgressMax(name: String, max: Long): Unit = {
    progressBars.get(name).foreach { bar =>
      progressBars(name) = bar.copy(max = max)
    }
    render()
  }

  def incrementProgress(name: String, amount: Long = 1): Unit = {
    progressBars.get(name).foreach { bar =>
      progressBars(name) = bar.copy(current = bar.current + amount)
    }
    render()
  }

  def incrementProgressMax(name: String, amount: Long): Unit = {
    progressBars.get(name).foreach { bar =>
      progressBars(name) = bar.copy(max = bar.max + amount)
    }
    render()
  }

  def log(message: String): Unit = {
    logs += message
    render()
  }

  def logInfo(message: String): Unit = {
    log(s"[INFO] $message")
  }
  
  def logWarn(message: String): Unit = {
    log(s"[WARN] $message")
  }
  
  def logDebug(message: String): Unit = {
    log(s"[DEBUG] $message")
  }
  
  def logError(message: String): Unit = {
    log(s"[ERROR] $message")
  }

  private def clearBoxLines(lines: Int): Unit = {
    for (_ <- 0 until lines) {
      cprint("\u001b[1A") // Move cursor up one line
      cprint("\u001b[2K") // Clear the entire line
    }
  }

  def render(): Unit = {
    val box = RichConsoleBoxWithLog(
      title = title,
      bars = progressBars.values.toSeq,
      logs = logs.toSeq,
      maxLogLines = maxLogLines,
      maxLines = maxProgressLines
    )

    val (output, lines) = box.render()
    clearBoxLines(lastLineCount)
    println(output)
    lastLineCount = lines
  }

  def finish(message: String = "✔ All tasks complete!"): Unit = {
    render() // Final render
    println(Str(message).overlay(Color.Green, 0, message.length))
  }

  def exit(message: String = "✘ Exiting..."): Nothing = {
    render() // Final render
    println(Str(message).overlay(Color.Red, 0, message.length))
    System.exit(1)
    throw new RuntimeException("Unreachable code after exit.")
  }

  def isComplete: Boolean = {
    progressBars.values.forall(bar => bar.current >= bar.max)
  }
}

trait BrickTask {
  def execute(context: LoggingCtx): Unit
}