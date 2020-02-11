package ic.org.arm
import arrow.core.Option

/**
 * Something that can be printed as ARM code
 */
interface Printable {
  val code: String
}

/**
 * ARM instruction, can be translated to ARM assembly code
 */
abstract class ARMInstr() : Printable {
  /**
   * Aliases to [Printable.code]
   */
  override fun toString() = code
}

/**
 * ARM instruction with a condition field (i.e. MOVEQ, BLT ..)
 */
abstract class ARMCondInstr() : ARMInstr() {
  abstract val cond: Option<CondFlag>

  /**
   * Returns opcode with condition flags
   */
  open fun opcode(op: String) = op + cond.fold({ "" }, { it.code })
}

/**
 * ARM instruction with a condition field (i.e. MOVEQ, BLT ..) and an S field to set flags
 */
abstract class ARMCondSInstr() : ARMCondInstr() {
  abstract val s: Boolean

  override fun opcode(op: String) = super.opcode(op) + if (s) "S" else ""
}

/**
 * Register, represented by an ID
 */
data class Reg(val id : Int) : Printable {
  override val code = "r$id"
}

/**
 * Label, represented by a name
 */
data class Label(val name: String) : Printable {
  override val code = ".$name"
}

/**
 * Sign for offsets
 */
sealed class Sign : Printable
object Plus : Sign() {
  override val code = ""
}
object Minus : Sign() {
  override val code = "-"
}
