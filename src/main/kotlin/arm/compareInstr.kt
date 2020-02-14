package ic.org.arm

import arrow.core.Option

data class CMPInstr(override val cond: Flag, val rn: Register, val op2: Operand2) : ARMCondInstr() {
  override val code = "${opcode("CMP")} ${rn.code}, ${op2.code}"
}
data class CMNInstr(override val cond: Flag, val rn: Register, val op2: Operand2) : ARMCondInstr() {
  override val code = "${opcode("CMN")} ${rn.code}, ${op2.code}"
}
