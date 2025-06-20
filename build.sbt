scalaVersion := "3.3.3" // A Long Term Support version.

// enablePlugins(ScalaNativePlugin)

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

// UTF-8 encoding for Java compiler
javacOptions ++= Seq("-encoding", "UTF-8")
scalacOptions ++= Seq("-encoding", "UTF-8")

// import to add Scala Native options
// import scala.scalanative.build._

// JVM-compatible dependency
libraryDependencies += "com.github.j-mie6" %% "parsley" % "5.0.0-M15"
libraryDependencies += "com.lihaoyi" %%% "fansi" % "0.5.0"
libraryDependencies += "org.jline" % "jline" % "3.29.0"

// defaults set with common options shown
// nativeConfig ~= { c =>
//   c.withLTO(LTO.none) // thin
//     .withMode(Mode.debug) // releaseFast
//     .withGC(GC.immix) // commix
// }
