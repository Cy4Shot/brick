scalaVersion := "3.3.3"

enablePlugins(ScalaNativePlugin)

javacOptions ++= Seq("-encoding", "UTF-8")
scalacOptions ++= Seq("-encoding", "UTF-8")

import scala.scalanative.build._

// JVM-compatible dependency
libraryDependencies += "com.github.j-mie6" %%% "parsley" % "5.0.0-M15"
libraryDependencies += "com.lihaoyi" %%% "fansi" % "0.5.0"
libraryDependencies += "com.lihaoyi" %%% "os-lib" % "0.11.4"

nativeConfig ~= {
  _.withLTO(LTO.none)
    .withOptimize(false)
    .withMode(Mode.debug)
    .withGC(GC.immix)
    .withSourceLevelDebuggingConfig(_.enableAll)
}
