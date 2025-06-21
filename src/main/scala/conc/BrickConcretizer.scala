package brick.conc

import java.io.File
import scala.collection.mutable

import brick.parse.BrickParser
import brick.parse.BrickAST.Program
import brick.parse.BrickAST
import brick.log.LoggingCtx

import parsley.{Success, Failure}

object BrickConcretizer {

  private def ensureBrickExtension(filename: String): String = {
    if (filename.endsWith(".brick")) filename else s"$filename.brick"
  }

  private def getDirectoryPath(filePath: String): String = {
    val file = new File(filePath)
    Option(file.getParent).getOrElse(".")
  }

  private def _concretize(input: String, root: String)(using
      log: LoggingCtx
  ): BrickTree = {
    if (root == input) {
      log.exit(s"Cyclic dependency detected for $input.")
    }

    val inputFileName =
      if (input == "Brickfile") input else ensureBrickExtension(input)
    val inputFile = new File(root, inputFileName)

    if (!inputFile.exists()) {
      log.exit(s"Brick file ${inputFile.getPath} does not exist.")
    }

    log.logInfo(s"Parsing Brick file: ${inputFile.getName}")

    val content =
      scala.io.Source.fromFile(inputFile).getLines().mkString("\n") + "\n"
    val res = BrickParser.parseString(content) match {
      case Failure(msg) =>
        log.exit(s"Error parsing Brick file:\n$msg")
      case Success(x) => x
    }

    log.logInfo(s"Analyzing Brick file: ${inputFile.getName}")

    val dependencies = res.stmts
      .collect { case BrickAST.DependenciesFlag(dependency) =>
        log.incrementProgressMax("parsing", 1)
        dependency.opt
      }
      .map(_concretize(_, root))

    val source = res.stmts.collect { case BrickAST.SourceFlag(sourceOpt) =>
      sourceOpt
    } match {
      case Nil         => log.exit(s"No source specified for $input.")
      case head :: Nil => head
      case head :: tail =>
        log.exit(s"Multiple sources specified for $input: ${head :: tail}.")
    }

    val envs = res.stmts
      .collect { case BrickAST.EnvironmentFlag(envOpt) => envOpt }
      .map(env => env.name -> env.value)
      .toMap

    val modules = res.stmts
      .collect { case BrickAST.ModulesFlag(modules) => modules }
      .flatten
      .map(module =>
        s"${module.name}${module.version.map(v => s"/$v").getOrElse("")}"
      )
      .toList

    val commands = res.stmts
      .collect { case BrickAST.CommandStmt(command) => command }
      .filter(_.nonEmpty)

    log.logInfo(s"Concretized Brick file: ${inputFile.getName}")
    log.incrementProgress("parsing", 1)

    BrickTree(
      name = input,
      dependencies = dependencies,
      brick = Brick(
        name = input,
        version = None,
        source = source,
        envs = envs,
        modules = modules,
        commands = commands
      )
    )
  }

  private def flattenToCompilationOrder(root: BrickTree): Bricks = {
    val visited = mutable.Set[String]()
    val result = mutable.ListBuffer[Brick]()

    def dfs(program: BrickTree): Unit = {
      if (!visited.contains(program.name)) {
        visited.add(program.name)
        program.dependencies.foreach(dfs)
        result.append(program.brick)
      }
    }

    dfs(root)
    result.toList
  }
  
  def concretize(
      root: String,
      input: Option[String] = None
  )(using log: LoggingCtx): Bricks = {
    val tree = input match {
      case Some(value) => _concretize(value, root)
      case None        => _concretize("Brickfile", root)
    }
    flattenToCompilationOrder(tree)
  }
}
