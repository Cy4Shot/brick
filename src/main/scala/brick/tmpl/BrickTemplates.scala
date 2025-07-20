package brick.tmpl

import brick.tmpl._
import brick.util.Platform
import brick.link.SystemInfo

def builtin = BrickTemplate {
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
