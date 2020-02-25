package ic.org.arm.instr

import ic.org.arm.ARMCondInstr
import ic.org.arm.Flag

data class NOPInstr(override val cond: Flag) : ARMCondInstr() {
  override val code = opcode("NOP")
}

data class YIELDInstr(override val cond: Flag) : ARMCondInstr() {
  override val code = opcode("YIELD")
}
