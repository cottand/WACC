package ic.org.arm

import ic.org.util.Code
import kotlinx.collections.immutable.persistentListOf

abstract class Exception {
  abstract val name: String
  abstract val body: Code
  val label by lazy { Label(name) }
}

object OverflowException : Exception() {
  override val name = "p_throw_overflow_error"

  private const val errormsg = "OverflowError: the result is too small/large to store in a 4-byte signed-integer.\\n"
  private val msg0 = StringData(errormsg, errormsg.length - 1)

  private val instructions by lazy {
    persistentListOf(
      label,
      LDRInstr(Reg.ret, ImmEqualLabel(msg0.label)),
      BLInstr(RuntimeError.label)
    )
  }

  override val body = Code(instructions, msg0.body)
}

object RuntimeError : Exception() {

  private val println = PrintStringStdFunc

  override val name = "p_throw_runtime_error"
  private val instructions by lazy {
    persistentListOf(
      label,
      BLInstr(println.label),
      LDRInstr(Reg.ret, -1),
      BLInstr("exit")
    )
  }
  override val body = Code(instr = instructions).withFunction(println.body)
}
