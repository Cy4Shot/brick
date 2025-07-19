package brick.tmpl.impl

import brick.tmpl._
import brick.util.Platform
import brick.link.SystemInfo

package object sys {
  @Template def sys = BrickTemplate {
    "sys" ~ {
      "arch" ~ {
        value(SystemInfo.arch)
      }
      "os" ~ {
        value(SystemInfo.os)
      }
      "kernel" ~ {
        value(SystemInfo.kernel)
      }
      "node" ~ {
        value(SystemInfo.node)
      }
      "kernel_version" ~ {
        value(SystemInfo.kernelVersion)
      }
      "platform" ~ {
        "windows" ~ {
          value(Platform.isWindows)
        }
        "linux" ~ {
          value(Platform.isLinux)
        }
        "mac" ~ {
          value(Platform.isMac)
        }
      }
    }
  }
}