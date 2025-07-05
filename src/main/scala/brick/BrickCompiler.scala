package brick

import brick.conc.{BrickConcretizer, Bricks}
import brick.gen.impl.{BricksGenerator, ConfigGenerator, MainGenerator, UtilsGenerator}
import brick.log.*

import java.nio.file.{Files, Path, Paths}

object BrickCompiler {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      printError("Usage: brick <init|gen> <name> [--flags]")
      return
    }

    val command = args(0)
    val nameOpt = args.lift(1)
    val flags = args.drop(2).toList

    command match {
      case "init" =>
        nameOpt match {
          case Some(name) =>
            val path = Paths.get(name)
            if (Files.exists(path)) {
              printError(s"Directory '$name' already exists.")
              return
            }
            try {
              Files.createDirectories(path)
              Files.createFile(path.resolve("Brickfile"))
              Files.createFile(path.resolve(s"$name.brick"))
              println(fansi.Color.Green(s"Initialized new brick project in '$name/'"))
            } catch {
              case e: Exception => printError(s"Failed to initialize: ${e.getMessage}")
            }
          case None =>
            printError("Usage: brick init <name>")
        }
      case "brick/genck/gen" =>
        nameOpt match {
          case Some(taskToRun) =>
            implicit val context: LoggingCtx = new LoggingCtx(s"Brick Compiler - Task: $taskToRun")
            context.addProgressBar("parsing", "Parsing BRICKs...", 1)
            context.addProgressBar("compiling", s"Compiling $taskToRun...", 1)
            context.logDebug(s"Initializing Brick Compiler for task: $taskToRun")
            try {
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
                MainGenerator(bricks).generateToFile(s"${bricks.name}/main")
              }
            } catch {
              case e: Exception => printError(s"Generation failed: ${e.getMessage}")
            }
          case None =>
            printError("Usage: brick gen <name>")
        }
      case _ =>
        printError("Unknown command. Use 'brick init <name>' or 'brick gen <name>'")
    }
  }
}