package ic.org.arm.instr

import arrow.core.None
import ic.org.arm.*

data class CMPInstr(override val cond: Flag = None, val rn: Register, val op2: Operand2) : ARMCondInstr() {
  constructor(cond: Flag, rn: Register, int8b: Short) : this(cond, rn, ImmOperand2(Immed_8r_bs(int8b.toByte())))
  constructor(rn: Register, int8b: Short) : this(None, rn, ImmOperand2(Immed_8r_bs(int8b.toByte())))

  override val code = "${opcode("CMP")} ${rn.code}, ${op2.code}"
}
