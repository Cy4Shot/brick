package brick.frontend

import parsley.Parsley
import parsley.position.pos
import parsley.ap._

object Bridges {

  type Pos = (Int, Int)

  /** A parser bridge for a parser with no parameters.
    * @tparam R
    *   The result type.
    */
  trait ParserBridgePos0[+R] {
    def apply(pos: Pos): R

    def apply(): R = apply((0, 0))

    /** Creates a parser that applies this bridge at the current position.
      * @param op
      *   The parser to apply.
      * @return
      *   The resulting parser.
      */
    final def <#(op: Parsley[Any]): Parsley[R] = pos.map(this.apply(_)) <~ op
  }

  /** A parser bridge for a parser with one parameter.
    * @tparam T1
    *   The type of the first parameter.
    * @tparam R
    *   The result type.
    */
  trait ParserBridgePos1[-T1, +R] {
    def apply(x: T1)(pos: Pos): R
    def apply(x: T1): R = apply(x)((0, 0))
    private def con(pos: Pos): T1 => R = this.apply(_)(pos)

    def apply(x: Parsley[T1]): Parsley[R] = ap1(pos.map(con), x)

    /** Creates a parser that applies this bridge at the current position.
      * @param op
      *   The parser to apply.
      * @return
      *   The resulting parser.
      */
    def from(op: Parsley[Any]): Parsley[T1 => R] = pos.map(con) <~ op
    final def <#(op: Parsley[Any]): Parsley[T1 => R] = this `from` op
  }

  /** A parser bridge for a parser with two parameters.
    * @tparam T1
    *   The type of the first parameter.
    * @tparam T2
    *   The type of the second parameter.
    * @tparam R
    *   The result type.
    */
  trait ParserBridgePos2[-T1, -T2, +R] {
    def apply(x1: T1, x2: T2)(pos: Pos): R
    def apply(x1: T1, x2: T2): R = apply(x1, x2)((0, 0))
    private def con(pos: Pos): (T1, T2) => R = this.apply(_, _)(pos)

    def apply(x1: Parsley[T1], x2: Parsley[T2]): Parsley[R] =
      ap2(pos.map(con), x1, x2)

    /** Creates a parser that applies this bridge at the current position.
      * @param op
      *   The parser to apply.
      * @return
      *   The resulting parser.
      */
    def from(op: Parsley[Any]): Parsley[(T1, T2) => R] = pos.map(con) <~ op
    final def <#(op: Parsley[Any]): Parsley[(T1, T2) => R] = this `from` op
  }

  /** A parser bridge for a parser with three parameters.
    * @tparam T1
    *   The type of the first parameter.
    * @tparam T2
    *   The type of the second parameter.
    * @tparam T3
    *   The type of the third parameter.
    * @tparam R
    *   The result type.
    */
  trait ParserBridgePos3[-T1, -T2, -T3, +R] {
    def apply(x1: T1, x2: T2, x3: T3)(pos: Pos): R
    def apply(x1: T1, x2: T2, x3: T3): R = apply(x1, x2, x3)((0, 0))
    private def con(pos: Pos): (T1, T2, T3) => R = this.apply(_, _, _)(pos)

    def apply(x1: Parsley[T1], x2: Parsley[T2], x3: Parsley[T3]): Parsley[R] =
      ap3(pos.map(con), x1, x2, x3)

    /** Creates a parser that applies this bridge at the current position.
      * @param op
      *   The parser to apply.
      * @return
      *   The resulting parser.
      */
    def from(op: Parsley[Any]): Parsley[(T1, T2, T3) => R] = pos.map(con) <~ op
    final def <#(op: Parsley[Any]): Parsley[(T1, T2, T3) => R] = this `from` op
  }

  /** A parser bridge for a parser with four parameters.
    * @tparam T1
    *   The type of the first parameter.
    * @tparam T2
    *   The type of the second parameter.
    * @tparam T3
    *   The type of the third parameter.
    * @tparam T4
    *   The type of the fourth parameter.
    * @tparam R
    *   The result type.
    */
  trait ParserBridgePos4[-T1, -T2, -T3, -T4, +R] {
    def apply(x1: T1, x2: T2, x3: T3, x4: T4)(pos: Pos): R
    def apply(x1: T1, x2: T2, x3: T3, x4: T4): R = apply(x1, x2, x3, x4)((0, 0))
    private def con(pos: Pos): (T1, T2, T3, T4) => R =
      this.apply(_, _, _, _)(pos)

    def apply(
        x1: Parsley[T1],
        x2: Parsley[T2],
        x3: Parsley[T3],
        x4: Parsley[T4]
    ): Parsley[R] = ap4(pos.map(con), x1, x2, x3, x4)

    /** Creates a parser that applies this bridge at the current position.
      * @param op
      *   The parser to apply.
      * @return
      *   The resulting parser.
      */
    def from(op: Parsley[Any]): Parsley[(T1, T2, T3, T4) => R] =
      pos.map(con) <~ op
    final def <#(op: Parsley[Any]): Parsley[(T1, T2, T3, T4) => R] =
      this `from` op
  }
}

