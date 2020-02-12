package ic.org.arm

import arrow.core.Option

data class MOVInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Register, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("MOV")} ${rd.code}, ${op2.code}"
}
data class MVNInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Register, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("MVN")} ${rd.code}, ${op2.code}"
}
data class TSTInstr(override val cond: Option<CondFlag>, val rn: Register, val op2: Operand2) : ARMCondInstr() {
  override val code = "${opcode("TST")} ${rn.code}, ${op2.code}"
}
data class TEQInstr(override val cond: Option<CondFlag>, val rn: Register, val op2: Operand2) : ARMCondInstr() {
  override val code = "${opcode("TEQ")} ${rn.code}, ${op2.code}"
}
data class ANDInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Register, val rn: Register, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("AND")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class EORInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Register, val rn: Register, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("EOR")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class ORRInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Register, val rn: Register, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("ORR")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class BICInstr(override val cond: Option<CondFlag>, override val s: Boolean, val rd: Register, val rn: Register, val op2: Operand2) : ARMCondSInstr() {
  override val code = "${opcode("BIC")} ${rd.code}, ${rn.code}, ${op2.code}"
}
data class CPYInstr(override val cond: Option<CondFlag>, val rd: Register, val rm: Register) : ARMCondInstr() {
  override val code = "${opcode("CPY")} ${rd.code}, ${rm.code}"
}
