package ic.org.arm

import arrow.core.Option

data class NOPInstr(override val cond: Option<CondFlag>) : ARMCondInstr() {
  override val code = opcode("NOP")
}
data class YIELDInstr(override val cond: Option<CondFlag>) : ARMCondInstr() {
  override val code = opcode("YIELD")
}
