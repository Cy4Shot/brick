package brick.gen.impl

import brick.gen.*
import brick.log.LoggingCtx
import brick.util.{IndentedStringBuilder, NVHPCConfig}

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

    if (threadOverride > 0) {
      threads = threadOverride.toString
    } else {
      threads = builder.threads
    }
  }

  def generate()(implicit builder: ScriptBuilder): String = {
    given config: IndentedStringBuilder = IndentedStringBuilder()
    
    builder.comment("Welcome to the Brick Config! You may edit this file to customize your build.")
    builder.newline()

    builder.comment("Directory Config")
    // TODO: Make this cross-platform
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
    builder.set("GLOBAL_FCFLAGS", flags)
    builder.set("NUM_THREADS", threads)
    builder.newline()

    if (nvhpc.isDefined) {
      builder.comment("NVHPC Config")
      builder.newline()
    }

    builder.comment("End of Brick Config")
    builder.out()
  }
}
