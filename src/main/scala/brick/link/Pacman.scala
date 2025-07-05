package brick.link

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@extern
object pacman {
  def get_package_manager(): CString = extern
}

object PacmanHelper {
  def getPackageManager: Option[String] =
    Zone {
      val result = pacman.get_package_manager()
      if (result == null) None else Some(fromCString(result))
    }
}