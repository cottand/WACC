package ic.org.arm

import arrow.core.None
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

  override val body = Code(instructions, msg0.body).withFunction(RuntimeError.body)
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

object CheckDivByZero : Exception() {
  override val name = "p_check_divide_by_zero"

  private const val errormsg = "DivideByZeroError: divide or modulo by zero\\n\\0"
  private val msg0 = StringData(errormsg, errormsg.length - 1)

  private val instructions by lazy {
    persistentListOf(
      label,
      PUSHInstr(LR),
      CMPInstr(None, Reg(1), ImmOperand2(Immed_8r(0, 0))),
      LDRInstr(EQCond, Reg(0), ImmEqualLabel(msg0.label)),
      BLInstr(EQCond, RuntimeError.label),
      POPInstr(PC)
    )
  }

  override val body = Code(instructions, msg0.body).withFunction(RuntimeError.body)
}

/**
 * First argument (r0) is array address (at which size is) and (r1) is array size
 */
object CheckArrayBounds : Exception() {
  override val name = "p_check_array_bounds"
  override val body: Code = TODO()
}

