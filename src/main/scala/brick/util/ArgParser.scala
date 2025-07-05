package brick.util

// Define enums for commands and options
enum BrickCommand(
    val value: String,
    val description: String,
    val pathDescription: String,
    val availableOptions: List[BrickOption],
    val examples: List[String]
) {
  case Init
      extends BrickCommand(
        "init",
        "Initialize a new brick project",
        "Directory path where the new project will be created",
        List(BrickOption.Help),
        List(
          "brick init my-project       Create project in 'my-project' directory",
          "brick init ./hpc-app        Create project in 'hpc-app' subdirectory"
        )
      )
  case Gen
      extends BrickCommand(
        "gen",
        "Generate scripts from brick files",
        "Path to the brick project or .brick file to compile",
        List(BrickOption.Help, BrickOption.Output),
        List(
          "brick gen examples/hpl                    Generate in current directory",
          "brick gen examples/hpl -o ./build         Generate in './build' directory",
          "brick gen my-app.brick --output /tmp/out   Generate in '/tmp/out' directory"
        )
      )
  case Help
      extends BrickCommand(
        "help",
        "Show help information",
        "",
        List.empty,
        List.empty
      )

  def usage: String = s"brick $value <path> [OPTIONS]"
}

object BrickCommand {
  def fromString(value: String): Option[BrickCommand] =
    values.find(_.value == value)

  def allCommands: List[BrickCommand] = values.toList.filter(_ != Help)
}

enum BrickOption(
    val shortFlag: String,
    val longFlag: String,
    val description: String,
    val takesValue: Boolean = false,
    val valueName: Option[String] = None
) {
  case Help extends BrickOption("-h", "--help", "Show this help message")
  case Output
      extends BrickOption(
        "-o",
        "--output",
        "Output directory for generated files (default: current directory)",
        takesValue = true,
        valueName = Some("<dir>")
      )
}

object BrickOption {
  def fromShortFlag(flag: String): Option[BrickOption] =
    values.find(_.shortFlag == flag)
  def fromLongFlag(flag: String): Option[BrickOption] =
    values.find(_.longFlag == flag)
}

case class ParsedArgs(
    command: BrickCommand,
    path: Option[String] = None,
    outputDir: Option[String] = None,
    flags: Map[BrickOption, String] = Map.empty,
    showHelp: Boolean = false
)

object ArgParser {
  def parse(args: Array[String]): Either[String, ParsedArgs] = {
    if (args.isEmpty) {
      return Left(generateShortUsage())
    }

    // Check for global help first
    if (args.length == 1 && (args(0) == "--help" || args(0) == "-h")) {
      return Right(
        ParsedArgs(BrickCommand.Help, None, None, Map.empty, showHelp = true)
      )
    }

    val commandStr = args(0)
    val command = BrickCommand.fromString(commandStr) match {
      case Some(cmd) => cmd
      case None =>
        return Left(
          s"Unknown command: $commandStr. Available commands: ${BrickCommand.allCommands
              .map(_.value)
              .mkString(", ")}\n${generateShortUsage()}"
        )
    }

    var i = 1
    var path: Option[String] = None
    var outputDir: Option[String] = None
    val flags = scala.collection.mutable.Map[BrickOption, String]()
    var showHelp = args.contains("-h") || args.contains("--help")

    // Parse positional argument (path) only if not showing help
    if (!showHelp && i < args.length && !args(i).startsWith("-")) {
      path = Some(args(i))
      i += 1
    }

    // Parse flags
    while (i < args.length) {
      val arg = args(i)
      arg match {
        case flag if flag.startsWith("--") =>
          BrickOption.fromLongFlag(flag) match {
            case Some(option) =>
              if (option == BrickOption.Help) {
                showHelp = true
                i += 1
              } else if (option.takesValue) {
                if (i + 1 >= args.length) {
                  return Left(
                    s"Option ${option.shortFlag}/${option.longFlag} requires a value"
                  )
                }
                if (option == BrickOption.Output) {
                  outputDir = Some(args(i + 1))
                } else {
                  flags(option) = args(i + 1)
                }
                i += 2
              } else {
                flags(option) = "true"
                i += 1
              }
            case None =>
              return Left(s"Unknown option: $flag")
          }
        case flag if flag.startsWith("-") =>
          BrickOption.fromShortFlag(flag) match {
            case Some(option) =>
              if (option == BrickOption.Help) {
                showHelp = true
                i += 1
              } else if (option.takesValue) {
                if (i + 1 >= args.length) {
                  return Left(
                    s"Option ${option.shortFlag}/${option.longFlag} requires a value"
                  )
                }
                if (option == BrickOption.Output) {
                  outputDir = Some(args(i + 1))
                } else {
                  flags(option) = args(i + 1)
                }
                i += 2
              } else {
                flags(option) = "true"
                i += 1
              }
            case None =>
              return Left(s"Unknown option: $flag")
          }
        case _ =>
          if (!showHelp) {
            return Left(s"Unexpected argument: ${args(i)}")
          }
          i += 1
      }
    }

    if (showHelp) {
      Right(ParsedArgs(command, path, outputDir, flags.toMap, showHelp = true))
    } else if (path.isEmpty && command != BrickCommand.Help) {
      Left(
        s"Usage: ${command.usage}\nUse 'brick ${command.value} --help' for more information."
      )
    } else {
      Right(ParsedArgs(command, path, outputDir, flags.toMap))
    }
  }

  def getHelpMessage(command: BrickCommand): String = {
    command match {
      case BrickCommand.Help => generateGeneralHelp()
      case _                 => generateCommandHelp(command)
    }
  }

  // Overloaded method for backward compatibility
  def getHelpMessage(commandStr: String): String = {
    BrickCommand.fromString(commandStr) match {
      case Some(cmd) => getHelpMessage(cmd)
      case None      => generateGeneralHelp()
    }
  }

  private def generateCommandHelp(cmd: BrickCommand): String = {
    val sb = new StringBuilder()

    sb.append(s"brick ${cmd.value} - ${cmd.description}\n\n")
    sb.append(s"USAGE:\n    ${cmd.usage}\n\n")

    if (cmd != BrickCommand.Help) {
      sb.append(s"ARGS:\n    <path>    ${cmd.pathDescription}\n\n")
    }

    val options = cmd.availableOptions
    if (options.nonEmpty) {
      sb.append("OPTIONS:\n")
      options.foreach { opt =>
        val valueStr = opt.valueName.getOrElse("")
        val flagStr = s"${opt.shortFlag}, ${opt.longFlag}${
            if (opt.takesValue) s" $valueStr" else ""
          }"
        sb.append(f"    $flagStr%-20s ${opt.description}\n")
      }
      sb.append("\n")
    }

    sb.append("DESCRIPTION:\n")
    cmd match {
      case BrickCommand.Init =>
        sb.append("    Creates a new brick project directory with:\n")
        sb.append("    - A Brickfile containing the target configuration\n")
        sb.append("    - An empty .brick file with the project name\n\n")
      case BrickCommand.Gen =>
        sb.append(
          "    Compiles brick files into executable shell scripts. The generated output includes:\n"
        )
        sb.append("    - Configuration scripts\n")
        sb.append("    - Utility functions\n")
        sb.append("    - Package-specific scripts\n")
        sb.append("    - Main execution script\n\n")
      case BrickCommand.Help =>
        sb.append("    Shows help information for brick commands.\n\n")
    }

    val examples = cmd.examples
    if (examples.nonEmpty) {
      sb.append("EXAMPLES:\n")
      examples.foreach { example =>
        sb.append(s"    $example\n")
      }
    }

    sb.toString
  }

  private def generateGeneralHelp(): String = {
    val sb = new StringBuilder()

    sb.append("brick - High-performance computing build system\n\n")
    sb.append("USAGE:\n    brick <COMMAND> [OPTIONS]\n\n")
    sb.append("COMMANDS:\n")

    BrickCommand.allCommands.foreach { cmd =>
      val shortUsage = generateCommandShortUsage(cmd)
      sb.append(f"    $shortUsage\n")
    }

    sb.append("\nOPTIONS:\n")
    sb.append("    -h, --help     Show help information\n\n")
    sb.append(
      "Use 'brick <command> --help' for more information on a specific command.\n"
    )

    sb.toString
  }

  private def generateShortUsage(): String = {
    val sb = new StringBuilder()
    sb.append("Usage: brick <COMMAND> [OPTIONS]\n\nAvailable commands:\n")
    BrickCommand.allCommands.foreach { cmd =>
      sb.append(s"  ${generateCommandShortUsage(cmd)}\n")
    }
    sb.append(
      "\nRun 'brick --help' for detailed help or 'brick <command> --help' for command-specific help."
    )
    sb.toString
  }

  private def generateCommandShortUsage(cmd: BrickCommand): String = {
    val options = cmd.availableOptions
      .filterNot(_ == BrickOption.Help)
      .map { opt =>
        if (opt.takesValue) {
          s"[${opt.shortFlag} ${opt.valueName.getOrElse("<value>")}]"
        } else {
          s"[${opt.shortFlag}]"
        }
      }
      .mkString(" ")

    val optionsStr = if (options.nonEmpty) s" $options" else ""
    s"brick ${cmd.value} <path>$optionsStr"
  }
}
