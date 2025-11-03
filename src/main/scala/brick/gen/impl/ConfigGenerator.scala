package brick.gen.impl

import brick.conc.BricksCtx
import brick.gen.*
import brick.util.{IndentedStringBuilder, NVHPCConfig}
import brick.util.Logging._

class ConfigGenerator(
    val ctx: BricksCtx,
    val threadOverride: Int = 0,
    val rootDirectory: String = "brick",
    val tmpDirectory: String = "tmp",
    val buildDirectory: String = "build",
    val installDirectory: String = "install"
) extends Generator {

  val cc = ctx.compilers.getOrElse("cc", "")
  val cxx = ctx.compilers.getOrElse("cxx", "")
  val fc = ctx.compilers.getOrElse("fc", "")
  val mpicc = ctx.compilers.getOrElse("mpicc", "")
  val mpicxx = ctx.compilers.getOrElse("mpicxx", "")
  val mpifc = ctx.compilers.getOrElse("mpifc", "")
  val cflags = ctx.compilerFlags.getOrElse("cflags", List())
  val cxxflags = ctx.compilerFlags.getOrElse("cxxflags", List())
  val fcflags = ctx.compilerFlags.getOrElse("fcflags", List())

  var compilerDir: String = ""
  var threads: String = ""

  def validate()(implicit builder: ScriptBuilder): Unit = {
    if (threadOverride < 0) {
      throwError("Thread override must be a non-negative integer.")
    }
    if (
      tmpDirectory.isEmpty || buildDirectory.isEmpty || installDirectory.isEmpty
    ) {
      throwError(
        "Directories cannot be empty. Please provide valid paths for tmp, build, and install directories."
      )
    }
    if (cc.isEmpty) printError("Compiler 'cc' is not set. Defaulting to gcc.")
    if (cxx.isEmpty) printError("Compiler 'cxx' is not set. Defaulting to g++.")
    if (fc.isEmpty)
      printWarn("Compiler 'fc' is not set. Defaulting to gfortran.")
    if (mpicc.isEmpty)
      printError("MPI C compiler 'mpicc' is not set. Defaulting to mpicc.")
    if (mpicxx.isEmpty)
      printError("MPI C++ compiler 'mpicxx' is not set. Defaulting to mpicxx.")
    if (mpifc.isEmpty)
      printWarn(
        "MPI Fortran compiler 'mpifc' is not set. Defaulting to mpif90."
      )

    compilerDir = Validator.checkExecutable(cc, "C compiler")

    if (threadOverride > 0) {
      threads = threadOverride.toString
    } else {
      threads = builder.threads
    }
  }

  def generate()(implicit builder: ScriptBuilder): String = {
    given config: IndentedStringBuilder = IndentedStringBuilder()

    builder.comment(
      "Welcome to the Brick Config! You may edit this file to customize your build."
    )
    builder.newline()

    builder.comment("Directory Config")
    builder.raw(s"export BRICKS_ROOT_DIR=$$(readlink -f ./$rootDirectory)")
    builder.set("TMP_DIR", "${BRICKS_ROOT_DIR}/" + tmpDirectory)
    builder.set("BUILD_DIR", "${BRICKS_ROOT_DIR}/" + buildDirectory)
    builder.set("INSTALL_DIR", "${BRICKS_ROOT_DIR}/" + installDirectory)
    builder.call("mkdir", "-p", "$TMP_DIR")
    builder.call("mkdir", "-p", "$BUILD_DIR")
    builder.call("mkdir", "-p", "$INSTALL_DIR")
    builder.newline()

    builder.comment("Compiler Config")
    builder.set("COMPILER_DIR", compilerDir)
    builder.set("CC", if (cc.nonEmpty) cc else "gcc")
    builder.set("CXX", if (cxx.nonEmpty) cxx else "g++")
    builder.set("FC", if (fc.nonEmpty) fc else "gfortran")
    builder.set("MPICC", if (mpicc.nonEmpty) mpicc else "mpicc")
    builder.set("MPICXX", if (mpicxx.nonEmpty) mpicxx else "mpicxx")
    builder.set("MPIFC", if (mpifc.nonEmpty) mpifc else "mpif90")
    builder.newline()

    builder.comment("Compilation Options")
    builder.set("GLOBAL_CFLAGS", cflags.mkString(" "))
    builder.set("GLOBAL_CXXFLAGS", cxxflags.mkString(" "))
    builder.set("GLOBAL_FCFLAGS", fcflags.mkString(" "))
    builder.set("NUM_THREADS", threads)
    builder.newline()

    builder.comment("End of Brick Config")
    builder.out()
  }
}
