package brick.parse


import parsley.Parsley
import parsley.token.{Basic, Lexer, Unicode}
import parsley.token.descriptions.*
import parsley.token.errors.*
import parsley.token.symbol.ImplicitSymbol

object BrickLexer {

  private val ident = Basic(c => c.toInt > 32 && c.toInt < 127 && c != '@' && c != ':' && c != '/')
  
  private val desc = LexicalDesc.plain.copy(
    nameDesc = NameDesc.plain.copy(
      identifierStart = ident,
      identifierLetter = ident,
    ),
    spaceDesc = SpaceDesc.plain.copy(
      lineCommentStart = "#",
      space = Unicode(c => Character.isWhitespace(c) && c != '\n' && c != '\r')
    ),
    symbolDesc = SymbolDesc.plain.copy(
      hardOperators = Set(
        "@", ":"
      )
    )
  )

  /* The error configuration for the lexer. */
  private val errorConfig = new ErrorConfig {

    /* Provide a custom label for invalid escape characters. */
    override def labelEscapeEnd: LabelWithExplainConfig = LabelAndReason(
      "valid escape sequences are: \\', \\\", \\\\, \\0, \\b, \\t, \\n, \\f, \\r",
      "end of escape sequence"
    )

    override def verifiedCharBadCharsUsedInLiteral: VerifiedBadChars =
      BadCharsReason(
        Map(
          '"'.toInt -> "double quote must be escaped as `\\\"`",
          '\''.toInt -> "single quote must be escaped as `\\'`",
          '\\'.toInt -> "backslash must be escaped as `\\\\`"
        )
      )

    /* Some custom labels for literals. */
    override def labelCharAsciiEnd: LabelConfig = Label(
      "end of character literal"
    )
    override def labelStringAsciiEnd(
        multi: Boolean,
        raw: Boolean
    ): LabelConfig = Label("end of string literal")
    override def labelStringCharacter: LabelConfig = Hidden
  }

  private val lexer = Lexer(desc, errorConfig)

  /** Parses an identifier.
    */
  val identifier: Parsley[String] = lexer.lexeme.names.identifier

  /** Implicit symbols.
    */
  val implicits: ImplicitSymbol = lexer.lexeme.symbol.implicits

  /** Helper function to expose the fully method of the lexer.
    * @param p
    *   The parser to apply.
    * @return
    *   The resulting parser.
    */
  def fully[A](p: Parsley[A]): Parsley[A] = lexer.fully(p)
}

