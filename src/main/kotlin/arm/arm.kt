package ic.org.arm
import arrow.core.Option
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus

/**
 * Something that can be printed as ARM code
 */
interface Printable {
  val code: String
}

/**
 * Highest level element of our IR. This compiler ends up producing a [List] of [Instr]s
 */
sealed class Instr : Printable {
  operator fun plus(other: Instr) = persistentListOf(this, other)
  operator fun plus(other: PersistentList<Instr>) = persistentListOf(this) + other
}

object CodeSegment : Instr() {
  override val code = ".global main"
}

object DataSegment : Instr() {
  override val code = ".text"
}

object LTORG : Instr() {
  override val code = ".ltorg"
}

/**
 * [Instr] that goes inside the Data segment of the program
 */
sealed class Data : Instr()

/**
 * ARM instruction, can be translated to ARM assembly code
 */
abstract class ARMInstr : Instr() {
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
abstract class ARMCondSInstr : ARMCondInstr() {
  abstract val s: Boolean

  override fun opcode(op: String) = super.opcode(op) + if (s) "S" else ""
}

sealed class Register : Printable

/**
 * Register, represented by an ID
 */
data class Reg(val id : Int) : Register() {
  init {
    require(id in 0..12)
  }
  override val code = "r$id"
}

object SP : Register() {
  override val code = "sp"
}
object LR : Register() {
  override val code = "lr"
}
object PC : Register() {
  override val code = "pc"
}

/**
 * Label, represented by a name
 */
data class Label(val name: String) : Instr() {
  override val code = "$name:"
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
