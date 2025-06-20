package brick.log

import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

@extern
object term {
  def get_terminal_width(): CInt = extern
  def get_terminal_height(): CInt = extern
  def get_terminal_size(rows: Ptr[CInt], cols: Ptr[CInt]): CInt = extern
}

object TerminalSize {
  import term._
  def getTerminalSize(): Option[(Int, Int)] = {
    try {
      Zone {
        val rows = alloc[CInt]()
        val cols = alloc[CInt]()

        if (get_terminal_size(rows, cols) == 0) {
          Some((!rows).toInt, (!cols).toInt)
        } else {
          None
        }
      }
    } catch {
      case _: Exception => None
    }
  }

  def getTerminalWidth(): Int = {
    try {
      get_terminal_width().toInt
    } catch {
      case _: Exception => 80
    }
  }

  def getTerminalHeight(): Int = {
    try {
      get_terminal_height().toInt
    } catch {
      case _: Exception => 24
    }
  }
}
