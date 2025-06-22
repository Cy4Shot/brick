package brick

import brick.log._
import brick.parse.BrickParser.parseString
import brick.conc.BrickConcretizer
import brick.conc.Bricks
import brick.gen.impl.{ConfigGenerator, UtilsGenerator}
import brick.gen.impl.BricksGenerator

import java.nio.file.{Files, Path, Paths}

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

    val tree: List[Bricks] = BrickConcretizer.concretize(taskToRun)

    for (bricks <- tree) {
      val path = Paths.get(bricks.name)
      if (Files.exists(path)) {
        def deleteRecursively(path: Path): Unit = {
          if (Files.isDirectory(path)) {
            Files.list(path).forEach(deleteRecursively)
          }
          Files.delete(path)
        }
        deleteRecursively(path)
      }
      context.logInfo(s"Compiling ${bricks.name} with ${bricks.bricks.size} bricks")
      ConfigGenerator("test").generateToFile(s"${bricks.name}/config")
      UtilsGenerator().generateToFile(s"${bricks.name}/utils")
      for (brick <- bricks.bricks) {
        context.logInfo(s"Compiling brick: ${brick.name}")
        BricksGenerator(brick).generateToFile(s"${bricks.name}/pkg/${brick.name}")
      }
    }
  }
}