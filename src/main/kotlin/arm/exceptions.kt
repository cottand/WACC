@file:Suppress("LeakingThis")

package ic.org.arm

import arrow.core.None
import ic.org.util.Code
import kotlinx.collections.immutable.persistentListOf
import java.util.Collections

abstract class Exception {
  abstract val name: String
  abstract val body: Code
  val label = Label(name)
}

object OverflowException : Exception() {
  override val name = "p_throw_overflow_error"

  private const val errormsg = "OverflowError: the result is too small/large to store in a 4-byte signed-integer.\n"
  private val msg0 = StringData("msg_0", errormsg, errormsg.length)

  private val instructions = persistentListOf(
    label,
    LDRInstr(Reg.ret, ImmEqualLabel(msg0.label)),
    BLInstr(RuntimeError.label)
  )

  override val body = Code(instructions, msg0.body)
}

object RuntimeError : Exception() {

  private const val format = "%.*s\\0"
  private val msg1 = StringData("msg_1", format, format.length - 1)

  override val name = "p_throw_runtime_error"

  private val printString = Label("p_print_string")

  private val instructions = persistentListOf(
    label,
    BLInstr(printString),
    LDRInstr(Reg.ret, -1),
    BLInstr("exit"),
    printString,
    PUSHInstr(LR),
    LDRInstr(Reg(1), Reg(0).zeroOffsetAddr),
    ADDInstr(None, false, Reg(2), Reg(0), 4),
    LDRInstr(Reg(0), ImmEqualLabel(msg1.label)),
    ADDInstr(None, false, Reg(0), Reg(0), 4),
    BLInstr("printf"),
    LDRInstr(Reg.ret, 0),
    BLInstr("fflush"),
    POPInstr(PC)
  )

  override val body = Code(instructions, msg1.body)

}
