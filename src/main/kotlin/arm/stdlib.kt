package ic.org.arm

import arrow.core.None
import ic.org.util.Code
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

abstract class StdFunc {
  abstract val name: String
  abstract val body: Code
  internal val label by lazy { Label(name) }
}

abstract class PrintLn : StdFunc() {
  abstract val msgTemplate: String
  internal val msg by lazy { StringData(msgTemplate, msgTemplate.length - 1) }
  override val body by lazy { Code(instructions, msg.body) }
  abstract val instructions: PersistentList<Instr>
}

object PrintIntStdFunc : PrintLn() {
  override val name = "p_print_int"
  override val msgTemplate = "%.d\\0"

  override val instructions by lazy {
    persistentListOf(
      label,
      PUSHInstr(LR),
      MOVInstr(rd = Reg(1), op2 = Reg(0)),
      LDRInstr(Reg.ret, ImmEqualLabel(msg.label)),
      ADDInstr(None, false, Reg(0), Reg(0), 4),
      BLInstr("printf"),
      LDRInstr(Reg.ret, 0),
      BLInstr("fflush"),
      POPInstr(PC)
    )
  }
}

object PrintStringStdFunc : PrintLn() {
  override val name = "p_print_string"
  override val msgTemplate = "%.*s\\0"

  override val instructions by lazy {
    persistentListOf(
      label,
      PUSHInstr(LR),
      LDRInstr(Reg(1), Reg(0).zeroOffsetAddr),
      ADDInstr(None, false, Reg(2), Reg(0), 4),
      LDRInstr(Reg(0), ImmEqualLabel(msg.label)),
      ADDInstr(None, false, Reg(0), Reg(0), 4),
      BLInstr("printf"),
      LDRInstr(Reg.ret, 0),
      BLInstr("fflush"),
      POPInstr(PC)
    )
  }
}
