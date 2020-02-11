package ic.org.arm

import arrow.core.Option

data class NOPInstr(override val cond: Option<CondFlag>) : ARMCondInstr(cond) {
  override val code = "NOP$condStr"
}
data class YIELDInstr(override val cond: Option<CondFlag>) : ARMCondInstr(cond) {
  override val code = "YIELD$condStr"
}