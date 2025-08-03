package brick.util

object Logging {

  def printSuccess(message: String): Unit = {
    println(fansi.Color.Green(message))
  }

  def printError(message: String): Unit = {
    println(fansi.Color.Red(message))
  }

  def printWarn(message: String): Unit = {
    println(fansi.Color.Yellow(message))
  }

  def throwError(message: String): Nothing = {
    printError(message)
    System.exit(1)
    throw new RuntimeException("Unreachable code after exit.")
  }
}
