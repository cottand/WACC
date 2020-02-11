package ic.org.arm
import arrow.core.Option

interface Printable {
  val code: String
}

abstract class ARMInstr : Printable {
  /**
   * Used to express conditional instructions
   */
  abstract var cond : Option<CondFlag>

  /**
   * Aliases to [Printable.code]
   */
  override fun toString() = code
}

data class Reg(var id : Int)

sealed class CondFlag

object EQCond : CondFlag() // Equal
object NECond : CondFlag() // Not Equal
object HSCond : CondFlag() // Unsigned higher or same
object CSCond : CondFlag() // Carry Set
object LOCond : CondFlag() // Unsigned lower
object CCCond : CondFlag() // Carry clear
object MICond : CondFlag() // Negative, Minus
object PLCond : CondFlag() // Positive or zero, plus
object VSCond : CondFlag() // Overflow
object VCCond : CondFlag() // No overflow
object HICond : CondFlag() // Unsigned higher
object LSCond : CondFlag() // Unsigned lower or same
object GECond : CondFlag() // Signed greater or equal
object LTCond : CondFlag() // Signed less than
object GTCond : CondFlag() // Signed greater than
object LECond : CondFlag() // Signed less than or equal
object ALCond : CondFlag() // Always

/**
 * 10-bit constant formed by left-shifting an 8-bit value by 2 bits
 */
data class Immed_8_4(val v: Byte)

/**
 * 8-bit constant
 */
data class Immed_8(val v: Byte)

/**
 * 5-bit constant
 */
data class Immed_5(val v: Byte)

/**
 * 32-bit constant formed by right-rotating an 8-bit value by an even number of bits
 */
data class Immed_8r(val v: Byte, val r: Byte)

