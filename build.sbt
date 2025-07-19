scalaVersion := "3.3.3"

enablePlugins(ScalaNativePlugin)

javacOptions ++= Seq("-encoding", "UTF-8")
scalacOptions ++= Seq("-encoding", "UTF-8")

import scala.scalanative.build._

// JVM-compatible dependency
libraryDependencies += "com.github.j-mie6" %%% "parsley" % "5.0.0-M15"
libraryDependencies += "com.lihaoyi" %%% "fansi" % "0.5.0"
libraryDependencies += "com.lihaoyi" %%% "os-lib" % "0.11.4"

Compile / sourceGenerators += Def.task {
  println(">> Generator started")
  val outDir = (Compile / sourceManaged).value / "brick" / "tmpl"
  IO.createDirectory(outDir)
  val outFile = outDir / "BrickTemplates.scala"
  val classesDir: File = (Compile / classDirectory).value
  val classpathUrls = Seq(classesDir.toURI.toURL)

  import _root_.io.github.classgraph.ClassGraph
  import scala.jdk.CollectionConverters._

  val annotationName = "brick.tmpl.Template"

  val scanResult = new ClassGraph()
    .enableAllInfo()
    .overrideClasspath(classpathUrls.asJava)
    .acceptPackages("brick")
    .scan()

  val allClasses = scanResult.getAllClasses.asScala

  val annotatedMembers = allClasses.flatMap { classInfo =>
    var className = classInfo.getName
    if (className.contains("$")) {
      Seq.empty
    } else {
      classInfo.getMethodInfo.asScala
        .filter(_.hasAnnotation(annotationName))
        .map { m =>
          if (className.endsWith(".package")) {
            className = className.dropRight(".package".length)
          }
          s"$className.${m.getName}"
        }
    }
  }

  println(s">> Found ${annotatedMembers.size} annotated members")

  val scalaCode =
    s"""
      |package brick.tmpl
      |
      |object BrickTemplates {
      |  val all: List[Node] = List(${annotatedMembers.mkString(", ")})
      |}
      |""".stripMargin

  IO.write(outFile, scalaCode)
  println(">> Scanning done and file written")
  Seq(outFile)
}.taskValue

nativeConfig ~= {
  _.withLTO(LTO.none)
    .withOptimize(false)
    .withMode(Mode.debug)
    .withGC(GC.immix)
    .withSourceLevelDebuggingConfig(_.enableAll)
}
