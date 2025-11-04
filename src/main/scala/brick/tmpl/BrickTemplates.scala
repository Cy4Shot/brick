package brick.tmpl

import brick.tmpl._
import brick.util.Platform
import brick.link.SystemInfo
import brick.parse.tmpl.Type._
import brick.conc.BricksCtx

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
  Seq("compiler", "comp") ~ {
    Seq("c", "cc") ~ {
      dynamic(_.compilers.getOrElse("cc", ""), TString())
    }
    Seq("cxx", "cpp", "c++") ~ {
      dynamic(_.compilers.getOrElse("cxx", ""), TString())
    }
    Seq("fortran", "fc", "f90") ~ {
      dynamic(_.compilers.getOrElse("fc", ""), TString())
    }
    Seq("mpic", "mpicc") ~ {
      dynamic(_.compilers.getOrElse("mpicc", ""), TString())
    }
    Seq("mpicxx", "mpic++", "mpicpp") ~ {
      dynamic(_.compilers.getOrElse("mpicxx", ""), TString())
    }
    Seq("mpifortran", "mpifc", "mpif90") ~ {
      dynamic(_.compilers.getOrElse("mpifc", ""), TString())
    }
    Seq("cflags", "c_flags") ~ {
      dynamic(_.compilerFlags.getOrElse("cflags", ""), TString())
    }
    Seq("cxxflags", "cxx_flags") ~ {
      dynamic(_.compilerFlags.getOrElse("cxxflags", ""), TString())
    }
    Seq("fcflags", "fc_flags") ~ {
      dynamic(_.compilerFlags.getOrElse("fcflags", ""), TString())
    }
  }
  "dir" ~ {
    Seq("install", "i") ~ {
      dynamic(b => s"$${${b.brick.name.toUpperCase}_INSTALL_DIR}", TVar())

      wildcard(TString()) { captured => (b: BricksCtx) =>
        if (b.bricks.find(_.name == captured).isEmpty) {
          throw new IllegalArgumentException(
            s"Wildcard dir.install: No such brick '${captured}'"
          )
        }
        s"$${${captured.toUpperCase}_INSTALL_DIR}"
      }
    }
    Seq("build", "b") ~ {
      dynamic(b => s"$${${b.brick.name.toUpperCase}_BUILD_DIR}", TVar())

      wildcard(TString()) { captured => (b: BricksCtx) =>
        if (b.bricks.find(_.name == captured).isEmpty) {
          throw new IllegalArgumentException(
            s"Wildcard dir.build: No such brick '${captured}'"
          )
        }
        s"$${${captured.toUpperCase}_BUILD_DIR}"
      }
    }
  }
}
