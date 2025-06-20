package brick.parse


import parsley.Parsley
import parsley.token.descriptions.*
import parsley.token.errors.*
import parsley.token.Basic
import parsley.token.Lexer
import parsley.token.Unicode

object BrickLexer {
  
  private val desc = LexicalDesc.plain.copy(
    nameDesc = NameDesc.plain.copy(
      identifierStart = Basic(c => c.toInt > 32 && c.toInt < 127 && c != '@' && c != ':' && c != '/'),
      identifierLetter = Basic(c => c.toInt > 32 && c.toInt < 127 && c != '@' && c != ':' && c != '/'),
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

    /* Provide a label for each symbol. */
    // override def labelSymbol: Map[String, LabelWithExplainConfig] =
    //   (unaryKeywords ++ unaryOperators)
    //     .map(_ -> Label("unary operator"))
    //     .toMap ++
    //     binaryOperators.map(_ -> Label("binary operator")).toMap

    /* Provide a custom label for integer literals. */
    override def labelIntegerSignedDecimal: LabelWithExplainConfig = Label(
      "integer"
    )

    /* Provide a error message out of bounds for integer literals. */
    override def filterIntegerOutOfBounds(
        min: BigInt,
        max: BigInt,
        nativeRadix: Int
    ): FilterConfig[BigInt] = new SpecializedMessage[BigInt] {
      def message(n: BigInt) = Seq(n match {
        case n if n < min => s"literal $n is smaller than min value of $min"
        case n if n > max => s"literal $n is larger than max value of $max"
        case _            => ""
      })
    }

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
    override def labelIntegerNumberEnd: LabelConfig = Label("end of integer")
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

  /** Parses an integer.
    */
  val integer = lexer.lexeme.integer.decimal32

  /** Parses a character.
    */
  val char = lexer.lexeme.character.ascii

  /** Parses a string.
    */
  val string = lexer.lexeme.string.ascii

  /** Parses an identifier.
    */
  val identifier = lexer.lexeme.names.identifier

  /** Implicit symbols.
    */
  val implicits = lexer.lexeme.symbol.implicits

  /** Helper function to expose the fully method of the lexer.
    * @param p
    *   The parser to apply.
    * @return
    *   The resulting parser.
    */
  def fully[A](p: Parsley[A]): Parsley[A] = lexer.fully(p)
}

