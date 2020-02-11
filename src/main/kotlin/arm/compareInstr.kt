package ic.org.arm

import arrow.core.Option

data class CMPInstr(override val cond: Option<CondFlag>, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "CMP$condStr ${rn.code}, ${op2.code}"
}
data class CMNInstr(override val cond: Option<CondFlag>, val rn: Reg, val op2: Operand2) : ARMCondInstr(cond) {
  override val code = "CMN$condStr ${rn.code}, ${op2.code}"
}
