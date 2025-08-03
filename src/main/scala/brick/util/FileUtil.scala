package brick.util

import os._

class FileUtil(val rootPath: os.Path) {

  def exists(sub: String = null): Boolean =
    if (sub != null) os.exists(rootPath / os.RelPath(sub)) else os.exists(rootPath)

  def write(content: String, sub: String = null, executable: Boolean = false): Unit = {
    val tgt = if (sub != null) rootPath / os.RelPath(sub) else rootPath
    os.makeDir.all(tgt / os.up)
    if (executable && Platform.isPosix) {
      os.write.over(tgt, content, "rwxr-xr-x")
    } else {
      os.write.over(tgt, content)
    }
  }

  def read(sub: String = null): String = {
    val tgt = if (sub != null) rootPath / os.RelPath(sub) else rootPath
    if (!os.exists(tgt)) throw new java.io.FileNotFoundException(s"File not found: $tgt")
    os.read(tgt)
  }

  def sub(sub: String): FileUtil = {
    new FileUtil(rootPath / os.RelPath(sub))
  }

  def name: String = rootPath.last.toString

  override def toString: String = rootPath.toString

  def asPath: os.Path = rootPath
}

object FileUtil {
  def apply(relPath: String): FileUtil = new FileUtil(pwd / os.RelPath(relPath))
}
