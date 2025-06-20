package brick

import scala.io.Source
import scala.collection.mutable
import java.io.File
import parsley.Failure
import parsley.Success
import brick.conc.BrickConcretizer

object BrickCompiler {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      throw new RuntimeException("Usage: brick <task-name> [--flags]")
    }

    val taskToRun = args(0)
    val flags = args.drop(1).toList

    println(BrickConcretizer.concretize("Brickfile", taskToRun))
  }
}