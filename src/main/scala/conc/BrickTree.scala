package brick.conc

import brick.parse.BrickAST.SourceOpt

case class BrickTree(
  name: String,
  version: Option[String],
  dependencies: List[BrickTree],
  source: SourceOpt,
  envs: Map[String, String],
  modules: List[String],
  commands: List[String]
)
