package ic.org.arm

import arrow.core.Option

data class ADDInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("ADD")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class ADCInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("ADC")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class SUBInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("SUB")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class SBCInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("SBC")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class RSBInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("RSB")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class RSCInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("RSC")} ${rd.code}, ${rn.code}, ${op2.code}"
}

data class MULInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rm: Reg, val rs: Reg) : ARMCondSInstr() {
  override val code = "${opcode("MUL")} ${rd.code}, ${rm.code}, ${rs.code}"
}
data class MLAInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Reg, val rm: Reg, val rs: Reg, val rn: Reg) : ARMCondSInstr() {
  override val code = "${opcode("MLA")} ${rd.code}, ${rm.code}, ${rs.code}, ${rn.code}"
}
