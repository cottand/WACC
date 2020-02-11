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
abstract class ARMCondInstr(open val cond: Option<CondFlag>) : ARMInstr() {
  val condStr = cond.fold({ "" }, { it.code })
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
