package brick.link

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@extern
object sysinfo {
  def sys_os(): CString = extern
  def sys_kernel(): CString = extern
  def sys_arch(): CString = extern
  def sys_kernel_version(): CString = extern
  def sys_node(): CString = extern
}

object SystemInfo {
  def os: String = Zone {
    fromCString(sysinfo.sys_os())
  }

  def kernel: String = Zone {
    fromCString(sysinfo.sys_kernel())
  }

  def arch: String = Zone {
    fromCString(sysinfo.sys_arch())
  }

  def kernelVersion: String = Zone {
    fromCString(sysinfo.sys_kernel_version())
  }

  def node: String = Zone {
    fromCString(sysinfo.sys_node())
  }
}
