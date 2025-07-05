package brick.link

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@extern
object gen {
  def which_executable(exe_name: CString): CString = extern
}

object GenHelper {
  def whichExecutable(exeName: String): Option[String] = {
    Zone { 
      val result = gen.which_executable(toCString(exeName))
      if (result == null) None else Some(fromCString(result))
    }
  }
}