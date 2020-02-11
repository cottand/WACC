package ic.org.arm
import arrow.core.Option
import ic.org.ast.Print

/**
 * Something that can be printed as ARM code
 */
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

data class Reg(var id : Int) : Printable {
  override val code = "r$id"
}

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
