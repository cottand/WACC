package ic.org.ast

import kotlin.math.pow

object Constatns {
  /**
   * Range which a WACC Int can represent. Beacuse its implementation is a 32 bit signed number,
   * it is the same range as the JVM Int primitive.
   */
  val intRange = Int.MIN_VALUE..Int.MAX_VALUE
}