package ic.org.arm

/* Push instructions */

data class PUSHInstr(val regs: List<Reg>) : ARMInstr() {
  override val code = "PUSH {${regs.joinToString(", ") { it.code }}}"
}
data class PUSHLRInstr(val regs: List<Reg>) : ARMInstr() {
  override val code = "PUSH {${regs.joinToString(", ") { it.code }}, LR}"
}

/* Pop instructions */

data class POPInstr(val regs: List<Reg>) : ARMInstr() {
  override val code = "POP {${regs.joinToString(", ") { it.code }}}"
}
data class POPPCInstr(val regs: List<Reg>) : ARMInstr() {
  override val code = "POP {${regs.joinToString(", ") { it.code }}, PC}"
}
