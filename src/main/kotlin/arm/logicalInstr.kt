package ic.org.arm

import arrow.core.Option

data class MOVInstr(override val cond: Option<CondFlag>, val rd: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "MOV$condStr ${rd.code}, ${op2.code}"
}
data class MVNInstr(override val cond: Option<CondFlag>, val rd: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "MVN$condStr ${rd.code}, ${op2.code}"
}
data class TSTInstr(override val cond: Option<CondFlag>, val rn: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "TST$condStr ${rn.code}, ${op2.code}"
}
data class TEQInstr(override val cond: Option<CondFlag>, val rn: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "TEQ$condStr ${rn.code}, ${op2.code}"
}
data class ANDInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "AND$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class EORInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "EOR$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class ORRInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "ORR$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class BICInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMInstr(cond) {
  override val code = "BIC$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class CPYInstr(override val cond: Option<CondFlag>, val rd: Reg, val rm: Reg) : ARMInstr(cond) {
  override val code = "CPY$condStr ${rd.code}, ${rm.code}"
}
