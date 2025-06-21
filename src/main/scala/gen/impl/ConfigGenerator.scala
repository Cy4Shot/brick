package brick.gen.impl

import scala.collection.mutable

import brick.gen._
import brick.log.LoggingCtx
import brick.util.NVHPCConfig

class ConfigGenerator(
    val name: String,
    val threadOverride: Int = 0,
    val rootDirectory: String = "brick",
    val tmpDirectory: String = "tmp",
    val buildDirectory: String = "build",
    val installDirectory: String = "install",
    val cc: String = "gcc",
    val cxx: String = "g++",
    var fc: String = "gfortran",
    val mpicc: String = "mpicc",
    val mpicxx: String = "mpicxx",
    val mpifc: String = "mpif90",
    val flags: String = "",
    val nvhpc: Option[NVHPCConfig] = None
)(implicit ctx: LoggingCtx)
    extends Generator {

  var compilerDir: String = ""
  var threads: String = ""

  def validate()(implicit builder: ScriptBuilder): Unit = {
    if (threadOverride < 0) {
      ctx.exit("Thread override must be a non-negative integer.")
    }
    if (
      tmpDirectory.isEmpty || buildDirectory.isEmpty || installDirectory.isEmpty
    ) {
      ctx.exit(
        "Directories cannot be empty. Please provide valid paths for tmp, build, and install directories."
      )
    }
    if (cc.isEmpty) ctx.logError("Compiler 'cc' is not set.")
    if (cxx.isEmpty) ctx.logError("Compiler 'cxx' is not set.")
    if (fc.isEmpty) ctx.logWarn("Compiler 'fc' is not set.")
    if (mpicc.isEmpty) ctx.logError("MPI C compiler 'mpicc' is not set.")
    if (mpicxx.isEmpty) ctx.logError("MPI C++ compiler 'mpicxx' is not set.")
    if (mpifc.isEmpty) ctx.logWarn("MPI Fortran compiler 'mpifc' is not set.")

    compilerDir = Validator.checkExecutable(cc, "C compiler")
    Validator.checkExecutable(cxx, "C++ compiler")

    if (threadOverride > 0) {
      threads = threadOverride.toString
    } else {
      threads = builder.threads
    }
  }

  def generate()(implicit builder: ScriptBuilder): String = {
    given config: mutable.StringBuilder = mutable.StringBuilder()
    
    builder.comment("Welcome to the Brick Config! You may edit this file to customize your build.")
    builder.newline()

    builder.comment("Directory Config")
    builder.set("BRICKS_ROOT_DIR", rootDirectory)
    builder.set("TMP_DIR", "${BRICKS_ROOT_DIR}/" + tmpDirectory)
    builder.set("BUILD_DIR", "${BRICKS_ROOT_DIR}/" + buildDirectory)
    builder.set("INSTALL_DIR", "${BRICKS_ROOT_DIR}/" + installDirectory)
    builder.newline()

    builder.comment("Compiler Config")
    builder.set("COMPILER_DIR", compilerDir)
    builder.set("CC", cc)
    builder.set("CXX", cxx)
    builder.set("FC", fc)
    builder.set("MPICC", mpicc)
    builder.set("MPICXX", mpicxx)
    builder.set("MPIFC", mpifc)
    builder.newline()

    builder.comment("Compilation Options")
    builder.set("GLOBAL_CFLAGS", flags)
    builder.set("GLOBAL_CXXFLAGS", flags)
    builder.set("GLOBAL_FFLAGS", flags)
    builder.set("NUM_THREADS", threads)
    builder.newline()

    if (nvhpc.isDefined) {
      builder.comment("NVHPC Config")
      builder.newline()
    }

    builder.comment("End of Brick Config")
    builder.newline()
    builder.newline()
    builder.comment("The following options should not be edited")

    builder.set("FMT_RESET", "\\e[0m")
    builder.set("FMT_COLOR_BLACK", "\\e[30m")
    builder.set("FMT_COLOR_RED", "\\e[31m")
    builder.set("FMT_COLOR_GREEN", "\\e[32m")
    builder.set("FMT_COLOR_YELLOW", "\\e[33m")
    builder.set("FMT_COLOR_BLUE", "\\e[34m")
    builder.set("FMT_COLOR_MAGENTA", "\\e[35m")
    builder.set("FMT_COLOR_CYAN", "\\e[36m")
    builder.set("FMT_COLOR_WHITE", "\\e[37m")
    builder.set("FMT_COLOR_BRIGHT_BLACK", "\\e[90m")
    builder.set("FMT_COLOR_BRIGHT_RED", "\\e[91m")
    builder.set("FMT_COLOR_BRIGHT_GREEN", "\\e[92m")
    builder.set("FMT_COLOR_BRIGHT_YELLOW", "\\e[93m")
    builder.set("FMT_COLOR_BRIGHT_BLUE", "\\e[94m")
    builder.set("FMT_COLOR_BRIGHT_MAGENTA", "\\e[95m")
    builder.set("FMT_COLOR_BRIGHT_CYAN", "\\e[96m")
    builder.set("FMT_COLOR_BRIGHT_WHITE", "\\e[97m")
    builder.set("FMT_BG_BLACK", "\\e[40m")
    builder.set("FMT_BG_RED", "\\e[41m")
    builder.set("FMT_BG_GREEN", "\\e[42m")
    builder.set("FMT_BG_YELLOW", "\\e[43m")
    builder.set("FMT_BG_BLUE", "\\e[44m")
    builder.set("FMT_BG_MAGENTA", "\\e[45m")
    builder.set("FMT_BG_CYAN", "\\e[46m")
    builder.set("FMT_BG_WHITE", "\\e[47m")
    builder.set("FMT_BG_BRIGHT_BLACK", "\\e[100m")
    builder.set("FMT_BG_BRIGHT_RED", "\\e[101m")
    builder.set("FMT_BG_BRIGHT_GREEN", "\\e[102m")
    builder.set("FMT_BG_BRIGHT_YELLOW", "\\e[103m")
    builder.set("FMT_BG_BRIGHT_BLUE", "\\e[104m")
    builder.set("FMT_BG_BRIGHT_MAGENTA", "\\e[105m")
    builder.set("FMT_BG_BRIGHT_CYAN", "\\e[106m")
    builder.set("FMT_BG_BRIGHT_WHITE", "\\e[107m")
    builder.set("FMT_BOLD", "\\e[1m")
    builder.set("FMT_DIM", "\\e[2m")
    builder.set("FMT_ITALIC", "\\e[3m")
    builder.set("FMT_UNDERLINE", "\\e[4m")
    builder.set("FMT_BLINK", "\\e[5m")
    builder.set("FMT_REVERSE", "\\e[7m")
    builder.set("FMT_HIDDEN", "\\e[8m")
    builder.set("FMT_STRIKETHROUGH", "\\e[9m")
    builder.set("FMT_RESET_BOLD_DIM", "\\e[22m")
    builder.set("FMT_RESET_ITALIC", "\\e[23m")
    builder.set("FMT_RESET_UNDERLINE", "\\e[24m")
    builder.set("FMT_RESET_BLINK", "\\e[25m")
    builder.set("FMT_RESET_REVERSE", "\\e[27m")
    builder.set("FMT_RESET_HIDDEN", "\\e[28m")
    builder.set("FMT_RESET_STRIKETHROUGH", "\\e[29m")

    builder.comment("End of Config")
    builder.out()
  }
}
