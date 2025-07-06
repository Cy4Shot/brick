package brick

import brick.conc.{BrickConcretizer, Bricks}
import brick.gen.impl.{BricksGenerator, ConfigGenerator, MainGenerator, UtilsGenerator}
import brick.link.ModuleSystem
import brick.log.*
import brick.util.{ArgParser, BrickCommand}
import os.*

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
      case BrickCommand.Gen | BrickCommand.Run =>
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
            if (!ModuleSystem.hasModuleSystem) {
              context.logWarn(
                "No module system detected. Some features may not work as expected."
              )
            }
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
            val path = MainGenerator(bricks).generateToFile(
              s"${bricks.name}/main",
              outputDir
            )

            if (parsedArgs.command == BrickCommand.Run) {
              os.call(
                cmd = "sh main.sh",
                cwd = os.Path(path, os.pwd) / os.up,
                stdin = os.Inherit,
                stdout = os.Inherit,
                stderr = os.Inherit
              )
            }
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
