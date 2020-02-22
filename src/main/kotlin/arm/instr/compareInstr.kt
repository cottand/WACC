package ic.org.arm.instr

import ic.org.arm.*

data class CMPInstr(override val cond: Flag, val rn: Register, val op2: Operand2) : ARMCondInstr() {
  constructor(cond: Flag, rn: Register, int8b: Short) : this(cond, rn,
    ImmOperand2(Immed_8r(int8b.toByte()))
  )
  override val code = "${opcode("CMP")} ${rn.code}, ${op2.code}"
}
data class CMNInstr(override val cond: Flag, val rn: Register, val op2: Operand2) : ARMCondInstr() {
  override val code = "${opcode("CMN")} ${rn.code}, ${op2.code}"
}
