package ic.org.arm

import arrow.core.Option

abstract class ARMInstr {
  /**
   * Returns the ARM assembly code for the instruction
   */
  abstract fun code() : String

  /**
   * Used to express conditional instructions
   */
  abstract var cond : Option<CondFlag>

  /**
   * Aliases to ARMInstr::code()
   */
  override fun toString(): String {
    return code()
  }
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