package brick.conc

import brick.util.Logging._
import brick.parse.BrickAST.{BasicOpt, Program}
import brick.parse.{BrickAST, BrickParser}
import parsley.{Failure, Success}

import java.io.File
import scala.collection.mutable
import scala.util.Using
import brick.util.FileUtil

object BrickConcretizer {

  private def ensureBrickExtension(filename: String): String = {
    if (filename.endsWith(".brick")) filename else s"$filename.brick"
  }

  private def getDirectoryPath(filePath: String): String = {
    val file = new File(filePath)
    Option(file.getParent).getOrElse(".")
  }

  private def _concretize(input: String, root: FileUtil): BrickTree = {
    val inputFileName =
      if (input == "Brickfile") input else ensureBrickExtension(input)
    val inputFile = root.sub(inputFileName)

    if (!inputFile.exists()) {
      throwError(s"Brick file $inputFile does not exist.")
    }

    val res = parseBrickFile(inputFile)

    val dependencies = res.stmts
      .collect { case BrickAST.DependenciesFlag(dependency) => dependency.opt }
      .map(_concretize(_, root))

    val source = res.stmts.collect { case BrickAST.SourceFlag(sourceOpt) =>
      sourceOpt
    } match {
      case Nil         => throwError(s"$input: No source specified.")
      case head :: Nil => head
      case head :: tail =>
        throwError(s"$input: Multiple sources specified: ${head :: tail}.")
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

    val commands = res.stmts
      .collect { case BrickAST.CommandStmt(command) => command }
      .filter(_.nonEmpty)

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

  private def flattenToCompilationOrder(root: BrickTree): List[Brick] = {
    val visited = mutable.Set[String]()
    val visiting = mutable.Set[String]()
    val result = mutable.ListBuffer[Brick]()

    def dfs(program: BrickTree): Unit = {
      if (visiting.contains(program.name)) {
        throwError(s"Cyclical dependency detected involving: ${program.name}")
      }

      if (!visited.contains(program.name)) {
        visiting.add(program.name)
        program.dependencies.foreach(dfs)
        visiting.remove(program.name)
        visited.add(program.name)
        result.append(program.brick)
      }
    }

    dfs(root)
    result.toList
  }

  private def parseRootBrickFile(inputFile: FileUtil): Program = {
    BrickParser.parseRootString(inputFile.read()) match {
      case Failure(msg) =>
        throwError(s"Error parsing $inputFile:\n$msg")
      case Success(x) => x
    }
  }

  private def parseBrickFile(inputFile: FileUtil): Program = {
    BrickParser.parseString(inputFile.read()) match {
      case Failure(msg) =>
        throwError(s"Error parsing $inputFile:\n$msg")
      case Success(x) => x
    }
  }

  def concretize(root: FileUtil): List[Bricks] = {
    if (!root.exists("Brickfile")) {
      throwError(s"Brickfile does not exist in $root.")
    }

    val res = parseRootBrickFile(root.sub("Brickfile"))
    val targets = res.stmts.collect { case BrickAST.TargetFlag(target) =>
      target
    }

    val packages = res.stmts
      .collect { case BrickAST.PackageFlag(BasicOpt(pkg)) => pkg }

    val compilers = res.stmts.collect {
      case BrickAST.CompilerFlag(compiler, BasicOpt(binary)) =>
        compiler.prettyPrint -> binary
    }.toMap

    val compilerFlags = res.stmts.collect {
      case BrickAST.CompilerFlagsFlag(flag, flags) =>
        flag.prettyPrint -> flags.map(_.opt)
    }.toMap

    if (targets.isEmpty) {
      throwError("No targets specified in Brickfile.")
    }

    // Concretize each target
    targets.map { target =>
      val tree = _concretize(target.opt, root)
      Bricks(
        target.opt,
        flattenToCompilationOrder(tree),
        packages,
        BricksCtx(compilers, compilerFlags)
      )
    }
  }
}
