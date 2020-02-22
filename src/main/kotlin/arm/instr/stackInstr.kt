package ic.org.arm.instr

import ic.org.arm.ARMInstr
import ic.org.arm.Register

/* Push instructions */

data class PUSHInstr(val regs: List<Register>) : ARMInstr() {
  constructor(reg: Register) : this(listOf(reg))

  override val code = "PUSH {${regs.joinToString(", ") { it.code }}}"
}

data class PUSHLRInstr(val regs: List<Register>) : ARMInstr() {
  override val code = "PUSH {${regs.joinToString(", ") { it.code }}, LR}"
}

/* Pop instructions */

data class POPInstr(val regs: List<Register>) : ARMInstr() {
  constructor(reg: Register) : this(listOf(reg))

  override val code = "POP {${regs.joinToString(", ") { it.code }}}"
}

data class POPPCInstr(val regs: List<Register>) : ARMInstr() {
  constructor(reg: Register) : this(listOf(reg))

  override val code = "POP {${regs.joinToString(", ") { it.code }}, PC}"
}
