package brick

import scala.io.Source
import scala.collection.mutable
import java.io.File
import brick.frontend.BrickParser
import parsley.Failure
import parsley.Success

object BrickCompiler {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      throw new RuntimeException("Usage: brick <task-name> [--flags]")
    }

    val taskToRun = args(0)
    val flags = args.drop(1).toList // Placeholder for future use

//    val scriptFile = new File("Brickfile")
//    if (!scriptFile.exists()) {
//      throw new RuntimeException("Error: Brickfile not found in current directory.")
//    }

    val res = BrickParser.parseString("""
@modules nvhpc-hpcx-cuda12/25.5 intel
@env PATH:/mnt/opt/gcc/15.1.0/bin

@source gh:UK-SCC/Cy4Shot:81972389e187fab1
@dep lua
@dep libxsmm
@dep boost
@dep parmetis

mkdir -p build
cd build
LDFLAGS="-lzip -lcurl" cmake ../ \
    -DHOST_ARCH=skx \
    -DCMAKE_BUILD_TYPE=Release \
    -DDEVICE_BACKEND=cuda \
    -DDEVICE_ARCH=sm_90 \
    -DNUMA_AWARE_PINNING=ON \
    -DASAGI=ON \
    -DGEMM_TOOLS_LIST=LIBXSMM,PSpaMM \
    -DPRECISION=double \
    -DORDER=4 \
    -DEQUATIONS=elastic \
    -DR_QUAD_RULE=dunavant \
    -DPLASTICITY_METHOD=nb \
    -DLTO=OFF \
    -DGRAPH_PARTITIONING_LIBS=parmetis \
    -DCMAKE_INSTALL_PREFIX=$SEISSOL_INSTALL_DIR

make -j$(nproc)
make install
""")

    res match  {
      case Failure(msg) =>
        println(s"Error parsing Brickfile: $msg")
        System.exit(1)
      case Success(x) =>
        println(x)
        println(x.prettyPrint)
    }
  }
}