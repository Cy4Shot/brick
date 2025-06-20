package brick

import scala.io.Source
import scala.collection.mutable
import java.io.File
import parsley.Failure
import parsley.Success
import brick.conc.BrickConcretizer

import brick.log._
import scala.concurrent.duration._
import scala.util.Random
import scala.Console.{print => cprint}
import fansi.Str
import fansi.Color
import brick.parse.BrickParser.parseString

object BrickCompiler {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      throw new RuntimeException("Usage: brick <task-name> [--flags]")
    }

    val taskToRun = args(0)
    val flags = args.drop(1).toList

    implicit val context = new LoggingCtx(s"Brick Compiler - Task: $taskToRun")
    
    // Initialize progress bars
    context.addProgressBar("parsing", "Parsing BRICKs...", 1)
    context.addProgressBar("compiling", s"Compiling $taskToRun...", 1)

    context.logDebug(s"Initializing Brick Compiler for task: $taskToRun")
    context.startAutoRender()

    BrickConcretizer.concretize("Brickfile", taskToRun)
  }
}