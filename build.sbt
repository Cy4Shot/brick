scalaVersion := "3.3.3" // A Long Term Support version.

// enablePlugins(ScalaNativePlugin)

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

// import to add Scala Native options
// import scala.scalanative.build._

// JVM-compatible dependency
libraryDependencies += "com.github.j-mie6" %% "parsley" % "5.0.0-M15"

// defaults set with common options shown
// nativeConfig ~= { c =>
//   c.withLTO(LTO.none) // thin
//     .withMode(Mode.debug) // releaseFast
//     .withGC(GC.immix) // commix
// }
