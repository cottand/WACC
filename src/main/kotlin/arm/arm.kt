package ic.org.arm
import arrow.core.Option
import ic.org.ast.Print

/**
 * Something that can be printed as ARM code
 */
interface Printable {
  val code: String
}

/**
 * ARM instruction, can be translated to ARM assembly code and has an optional cond field
 */
abstract class ARMInstr(open val cond: Option<CondFlag>) : Printable {
  val condStr = cond.fold({ "" }, { it.code })

  /**
   * Aliases to [Printable.code]
   */
  override fun toString() = code
}

/**
 * Register, represented by an ID
 */
data class Reg(val id : Int) : Printable {
  override val code = "r$id"
}

data class Label(val name: String) : Printable {
  override val code = ".$name"
}
