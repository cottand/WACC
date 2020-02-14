package ic.org.arm

import arrow.core.Option

data class NOPInstr(override val cond: Flag) : ARMCondInstr() {
  override val code = opcode("NOP")
}
data class YIELDInstr(override val cond: Flag) : ARMCondInstr() {
  override val code = opcode("YIELD")
}
