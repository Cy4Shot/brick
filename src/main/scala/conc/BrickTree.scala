package brick.conc

import brick.parse.BrickAST.SourceOpt

type Bricks = List[Brick]

case class BrickTree(
  name: String,
  dependencies: List[BrickTree],
  brick: Brick
)

case class Brick(
  name: String,
  version: Option[String],
  source: SourceOpt,
  envs: Map[String, String],
  modules: List[String],
  commands: List[String]
)
