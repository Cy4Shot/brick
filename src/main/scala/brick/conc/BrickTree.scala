package brick.conc

import brick.parse.BrickAST.SourceOpt

case class BrickTree(
  name: String,
  dependencies: List[BrickTree],
  brick: Brick,
)

case class Brick(
  name: String,
  version: Option[String],
  source: SourceOpt,
  envs: Map[String, String],
  modules: List[String],
  commands: List[String],
)

case class Bricks(
  name: String,
  bricks: List[Brick],
  packages: List[String],
)
