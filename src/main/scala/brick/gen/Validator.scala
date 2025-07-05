package brick.gen

import brick.log.*

object Validator {
  def checkExecutable(
      executable: String,
      compilerType: String,
      required: Boolean = true
  )(implicit ctx: LoggingCtx): String =
    brick.link.GenHelper.whichExecutable(executable) match {
      case Some(path) => path
      case None =>
        if (required)
          ctx.exit(
            s"$compilerType $executable not found in PATH. Please ensure it is installed and available."
          )
        else {
          ctx.logWarn(
            s"$compilerType $executable not found in PATH. Continuing without it."
          )
          ""
        }
    }
}