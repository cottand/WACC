package ic.org.arm

import arrow.core.Option

data class ADDInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "ADD$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class ADCInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "ADC$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class SUBInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "SUB$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class SBCInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "SBC$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class RSBInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "RSB$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}
data class RSCInstr(override val cond: Option<CondFlag>, val rd: Reg, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "RSC$condStr ${rd.code}, ${rn.code}, ${op2.code}"
}

data class MULInstr(override val cond: Option<CondFlag>, val rd: Reg, val rm: Reg, val rs: Reg) : ARMCondInstr(cond) {
  override val code = "MUL$condStr ${rd.code}, ${rm.code}, ${rs.code}"
}
data class MLAInstr(override val cond: Option<CondFlag>, val rd: Reg, val rm: Reg, val rs: Reg, val rn: Reg) : ARMCondInstr(cond) {
  override val code = "MUL$condStr ${rd.code}, ${rm.code}, ${rs.code}, ${rn.code}"
}
