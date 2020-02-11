package ic.org.arm
import arrow.core.Option
import ic.org.ast.Print

/**
 * Something that can be printed as ARM code
 */
interface Printable {
  val code: String
}

abstract class ARMInstr(open val cond: Option<CondFlag>) : Printable {
  val condStr = cond.fold({ "" }, { it.code })

  /**
   * Aliases to [Printable.code]
   */
  override fun toString() = code
}

data class Reg(var id : Int) : Printable {
  override val code = "r$id"
}
