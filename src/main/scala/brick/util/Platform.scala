package brick.util

import scala.scalanative.runtime.Platform as NativePlatform

object Platform {
  def isWindows: Boolean = NativePlatform.isWindows()
  def isLinux: Boolean = NativePlatform.isLinux()
  def isMac: Boolean = NativePlatform.isMac()
  def isPosix: Boolean = isLinux || isMac
}
