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
interface ARMAsmInstr {
  val code: String
  operator fun plus(other: PersistentList<ARMAsmInstr>) = persistentListOf(this) + other
}

data class AsmDirective(val text: String) : Data() {
  override val code = ".$text"

  companion object {
    val main = AsmDirective("global main")
    val data = AsmDirective("data")
    val text = AsmDirective("text")
    val ltorg = AsmDirective("ltorg")
  }
}

/**
 * [ARMAsmInstr] that goes inside the Data segment of the program
 */
sealed class Data : ARMAsmInstr

/**
 * ARM instruction, can be translated to ARM assembly code
 */
abstract class ARMInstr : ARMAsmInstr {
  /**
   * Aliases to [ARMAsmInstr.code]
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

sealed class Register : ARMAsmInstr

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
data class AsmLabel(val name: String) : Data() {
  override val code = "$name:"
}

/**
 * Sign for offsets
 */
sealed class Sign : ARMAsmInstr

object Plus : Sign() {
  override val code = ""
}

object Minus : Sign() {
  override val code = "-"
}

annotation class ARMGenOnly

@Deprecated("Only for quick testing")
data class InlineARM(override val code: String) : ARMInstr()
