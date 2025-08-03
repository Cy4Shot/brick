package brick.gen

import brick.util.Logging._

object Validator {
  def checkExecutable(
      executable: String,
      compilerType: String,
      required: Boolean = true
  ): String =
    brick.link.GenHelper.whichExecutable(executable) match {
      case Some(path) => path
      case None =>
        if (required)
          throwError(
            s"$compilerType $executable not found in PATH. Please ensure it is installed and available."
          )
        else {
          printWarn(
            s"$compilerType $executable not found in PATH. Continuing without it."
          )
          ""
        }
    }
}
