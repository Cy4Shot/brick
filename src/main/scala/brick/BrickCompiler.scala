package brick

import brick.conc.{BrickConcretizer, Bricks}
import brick.gen.impl.{BricksGenerator, ConfigGenerator, MainGenerator, UtilsGenerator}
import brick.log.*
import brick.util.{ArgParser, BrickCommand}

import java.nio.file.{Files, Path, Paths}

object BrickCompiler {

  def main(args: Array[String]): Unit = {
    val parsedArgs = ArgParser.parse(args) match {
      case Left(error) =>
        printError(error)
        return
      case Right(parsed) => parsed
    }

    // Handle help requests
    if (parsedArgs.showHelp) {
      println(ArgParser.getHelpMessage(parsedArgs.command))
      return
    }

    parsedArgs.command match {
      case BrickCommand.Init =>
        val pathStr = parsedArgs.path.get
        val path = Paths.get(pathStr)
        val name = path.getFileName.toString
        if (Files.exists(path)) {
          printError(s"Directory '$pathStr' already exists.")
          return
        }
        try {
          Files.createDirectories(path)
          Files.write(
            path.resolve("Brickfile"),
            s"@target $name".getBytes
          )
          Files.createFile(path.resolve(s"$name.brick"))
          println(
            fansi.Color.Green(
              s"Initialized new brick project in '$pathStr/'"
            )
          )
        } catch {
          case e: Exception =>
            printError(s"Failed to initialize: ${e.getMessage}")
        }
      case BrickCommand.Gen =>
        val pathStr = parsedArgs.path.get
        val outputDir = parsedArgs.outputDir.getOrElse(".")
        val taskToRun = Paths.get(pathStr).getFileName.toString
        implicit val context: LoggingCtx = new LoggingCtx(
          s"Brick Compiler - Task: $taskToRun"
        )
        context.addProgressBar("parsing", "Parsing BRICKs...", 1)
        context.addProgressBar("compiling", s"Compiling $taskToRun...", 1)
        context.logDebug(
          s"Initializing Brick Compiler for task: $taskToRun at path: $pathStr, output: $outputDir"
        )
        try {
          val tree: List[Bricks] = BrickConcretizer.concretize(pathStr)
          for (bricks <- tree) {
            val targetPath = Paths.get(outputDir, bricks.name)
            if (Files.exists(targetPath)) {
              def deleteRecursively(path: Path): Unit = {
                if (Files.isDirectory(path)) {
                  Files.list(path).forEach(deleteRecursively)
                }
                Files.delete(path)
              }
              deleteRecursively(targetPath)
            }
            context.logInfo(
              s"Compiling ${bricks.name} with ${bricks.bricks.size} bricks to $outputDir"
            )
            ConfigGenerator("test").generateToFile(
              s"${bricks.name}/config",
              outputDir
            )
            UtilsGenerator().generateToFile(s"${bricks.name}/utils", outputDir)
            for (brick <- bricks.bricks) {
              context.logInfo(s"Compiling brick: ${brick.name}")
              BricksGenerator(brick).generateToFile(
                s"${bricks.name}/pkg/${brick.name}",
                outputDir
              )
            }
            MainGenerator(bricks).generateToFile(
              s"${bricks.name}/main",
              outputDir
            )
          }
        } catch {
          case e: Exception =>
            printError(s"Generation failed: ${e.getMessage}")
        }
      case BrickCommand.Help =>
        // Help is already handled above
        ()
    }
  }
}
