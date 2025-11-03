package brick.tmpl

import brick.tmpl._
import brick.util.Platform
import brick.link.SystemInfo
import brick.parse.tmpl.Type._

def builtin = BrickTemplate {
  "sys" ~ {
    "arch" ~ {
      value(SystemInfo.arch, TString())
    }
    "os" ~ {
      value(SystemInfo.os, TString())
    }
    "kernel" ~ {
      value(SystemInfo.kernel, TString())
    }
    "node" ~ {
      value(SystemInfo.node, TString())
    }
    "kernel_version" ~ {
      value(SystemInfo.kernelVersion, TString())
    }
    "platform" ~ {
      "windows" ~ {
        value(Platform.isWindows, TBool())
      }
      "linux" ~ {
        value(Platform.isLinux, TBool())
      }
      "mac" ~ {
        value(Platform.isMac, TBool())
      }
    }
    "nproc" ~ {
      value("${NUM_THREADS}", TVar())
    }
  }
}
