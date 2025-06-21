package brick

import brick.log._
import brick.parse.BrickParser.parseString
import brick.conc.BrickConcretizer
import brick.conc.Bricks
import brick.gen.impl.{ConfigGenerator, UtilsGenerator}
import brick.gen.impl.BricksGenerator

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

    val tree: Bricks = BrickConcretizer.concretize(taskToRun)

    ConfigGenerator("test").generateToFile("config")
    UtilsGenerator().generateToFile("utils")
    BricksGenerator(tree(1)).generateToFile("bricks")
  }
}