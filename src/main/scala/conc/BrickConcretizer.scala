package brick.conc

import java.io.File
import brick.parse.BrickParser
import parsley.{Success, Failure}
import brick.parse.BrickAST.Program
import brick.parse.BrickAST

object BrickConcretizer {

  private def ensureBrickExtension(filename: String): String = {
    if (filename.endsWith(".brick")) filename else s"$filename.brick"
  }

  private def getDirectoryPath(filePath: String): String = {
    val file = new File(filePath)
    Option(file.getParent).getOrElse(".")
  }

  def concretize(input: String, root: String): BrickTree = {
    if (root == input) {
      throw new RuntimeException(s"Cyclic dependency detected for $input.")
    }

    val inputFileName = if (input == "Brickfile") input else ensureBrickExtension(input)
    val inputFile = new File(root, inputFileName)
    
    if (!inputFile.exists()) {
      throw new RuntimeException(s"Brick file ${inputFile.getPath} does not exist.")
    }

    val content = scala.io.Source.fromFile(inputFile).getLines().mkString("\n") + "\n"
    val res = BrickParser.parseString(content) match {
      case Failure(msg) =>
        throw new RuntimeException(s"Error parsing Brick file:\n$msg")
      case Success(x) => x
    }

    println(res)

    val dependencies = res.stmts
      .collect { case BrickAST.DependenciesFlag(dependency) =>
        dependency.opt
      }.map(concretize(_, root))

    val source = res.stmts.collect {
      case BrickAST.SourceFlag(sourceOpt) => sourceOpt
    } match {
      case Nil => throw new RuntimeException(s"No source specified for $input.")
      case head :: Nil => head
      case head :: tail =>
        throw new RuntimeException(s"Multiple sources specified for $input: ${head :: tail}.")
    }

    val envs = res.stmts
      .collect { case BrickAST.EnvironmentFlag(envOpt) => envOpt }
      .map(env => env.name -> env.value).toMap

    val modules = res.stmts
      .collect { case BrickAST.ModulesFlag(modules) => modules }
      .flatten
      .map(module => s"${module.name}${module.version.map(v => s"/$v").getOrElse("")}")
      .toList

    val commands = res.stmts
      .collect { case BrickAST.CommandStmt(command) => command }
      .filter(_.nonEmpty)

    BrickTree(
      name = input,
      version = None,
      dependencies = dependencies,
      source = source,
      envs = envs,
      modules = modules,
      commands = commands
    )
  }
}