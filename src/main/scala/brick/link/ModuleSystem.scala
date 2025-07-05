package brick.link

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@extern
object module {
  def has_module_system(): CInt = extern
}

object ModuleSystem {
  def hasModuleSystem: Boolean = Zone {
    module.has_module_system() == 1
  }
}
