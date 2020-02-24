package ic.org.arm

import arrow.core.None
import arrow.core.some
import ic.org.util.NOT_REACHED
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList

typealias Regs = PersistentList<Reg>

/**
 * Something that can be printed as ARM code
 */
interface Printable {
  val code: String
}

/**
 * Highest level element of our IR. This compiler ends up producing a [List] of [Instr]s
 */
interface Instr : Printable {
  operator fun plus(other: Instr) = persistentListOf(this, other)
  operator fun plus(other: PersistentList<Instr>) = persistentListOf(this) + other
}

data class Directive(val text: String) : Data() {
  override val code = ".$text"

  companion object {
    val main = Directive("global main")
    val data = Directive("data")
    val text = Directive("text")
    val ltorg = Directive("ltorg")
  }
}

/**
 * [Instr] that goes inside the Data segment of the program
 */
sealed class Data : Instr

/**
 * ARM instruction, can be translated to ARM assembly code
 */
abstract class ARMInstr : Instr {
  /**
   * Aliases to [Printable.code]
   */
  override fun toString() = code
}

/**
 * ARM instruction with a condition field (i.e. MOVEQ, BLT ..)
 */
abstract class ARMCondInstr() : ARMInstr() {
  abstract val cond: Flag

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
data class Reg(val id: Int) : Register() {
  init {
    require(id in 0..12)
  }

  override val code = "r$id"

  /**
   * Returns the next highest numbered [Register.some], unless the next [Register] is the 12th.
   * In that case, returns [None].
   *
   * Should never be called on [Reg.last]
   */
  val next by lazy {
    when (id) {
      in 0..10 -> Reg(id + 1).some()
      11 -> None
      else -> NOT_REACHED()
    }
  }

  companion object {
    val last = Reg(12)
    val first = Reg(0)
    val firstExpr = Reg(4)
    val ret = first
    val fstArg = first
    val sndArg = Reg(1)
    /**
     * All registers available by default, excluding [Reg.last]
     */
    val all by lazy {
      (0..11).map { Reg(it) }.toPersistentList()
    }
    val fromExpr by lazy {
      (4..11).map { Reg(it) }.toPersistentList()
    }
  }
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
data class Label(val name: String) : Data() {
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

@Deprecated("Only for quick testing")
data class InlineARM(override val code: String) : ARMInstr()
