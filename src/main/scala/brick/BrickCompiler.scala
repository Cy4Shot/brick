package brick

import brick.conc.{BrickConcretizer, Bricks}
import brick.gen.impl._
import brick.link.ModuleSystem
import brick.util.Logging._
import brick.util.{ArgParser, BrickCommand}
import os.*

import java.nio.file.{Files, Path, Paths}
import brick.util.FileUtil
import scala.scalanative.meta.LinktimeInfo.target

object BrickCompiler {

  def main(args: Array[String]): Unit = {
    val parsedArgs = ArgParser.parse(args) match {
      case Left(error) =>
        throwError(error)
        return
      case Right(parsed) => parsed
    }

    if (parsedArgs.showHelp) {
      println(ArgParser.getHelpMessage(parsedArgs.command))
      return
    }

    parsedArgs.command match {
      case BrickCommand.Init =>
        val brickDir = FileUtil(parsedArgs.path.get)
        if (brickDir.exists()) {
          throwError(s"Directory '$brickDir' already exists.")
        }
        val name = brickDir.name
        try {
          brickDir.write(s"@target $name\n", "Brickfile")
          brickDir.write("", s"$name.brick")
          printSuccess(s"Initialized new brick project in '$brickDir/'")
        } catch {
          case e: Exception =>
            throwError(s"Failed to initialize: ${e.getMessage}")
        }
      case BrickCommand.Gen | BrickCommand.Run =>
        val brickDir = FileUtil(parsedArgs.path.get)
        val outputDir = FileUtil(parsedArgs.outputDir.getOrElse("."))
        try {
          val tree: List[Bricks] = BrickConcretizer.concretize(brickDir)
          for (bricks <- tree) {
            val targetPath = outputDir.sub(bricks.name)
            ConfigGenerator(bricks.ctx).write(targetPath, "config")
            UtilsGenerator().write(targetPath, "utils")
            for (brick <- bricks.bricks) {
              BricksGenerator(brick, bricks.ctx)
                .write(targetPath.sub("pkg"), brick.name)
            }
            MainGenerator(bricks).write(targetPath, "main", true)
            printSuccess(
              s"Generated brick project in '$targetPath/'"
            )

            if (parsedArgs.command == BrickCommand.Run) {
              try
                os.call(
                  cmd = Seq("bash", "main.sh"),
                  cwd = targetPath.asPath,
                  stdin = os.Inherit,
                  stdout = os.Inherit,
                  stderr = os.Inherit
                )
              catch
                case e: os.SubprocessException =>
                  println(
                    s"Command failed with exit code: ${e.result.exitCode}"
                  )
                  println("STDOUT:")
                  println(e.result.out.text())
                  println("STDERR:")
                  println(e.result.err.text())
            }
          }
        } catch {
          case e: Exception =>
            e.printStackTrace()
            throwError(s"Generation failed: ${e.getMessage}")
        }
      case BrickCommand.Help =>
        // Help is already handled above
        ()
    }
  }
}
