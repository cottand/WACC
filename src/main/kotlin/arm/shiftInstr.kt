package ic.org.arm

data class LSLImmInstr(val rd: Reg, val rm: Reg, val imm: Immed_5) : ARMInstr() {
  override val code = "LSL ${rd.code}, ${rm.code}, #${imm.code}"
}
data class LSLRegInstr(val rd: Reg, val rs: Reg) : ARMInstr() {
  override val code = "LSL ${rd.code}, ${rs.code}"
}

data class LSRImmInstr(val rd: Reg, val rm: Reg, val imm: Immed_5) : ARMInstr() {
  override val code = "LSR ${rd.code}, ${rm.code}, #${imm.code}"
}
data class LSRRegInstr(val rd: Reg, val rs: Reg) : ARMInstr() {
  override val code = "LSR ${rd.code}, ${rs.code}"
}

data class ASRImmInstr(val rd: Reg, val rm: Reg, val imm: Immed_5) : ARMInstr() {
  override val code = "ASR ${rd.code}, ${rm.code}, #${imm.code}"
}
data class ASRRegInstr(val rd: Reg, val rs: Reg) : ARMInstr() {
  override val code = "ASR ${rd.code}, ${rs.code}"
}

data class RORInstr(val rd: Reg, val rs: Reg) : ARMInstr() {
  override val code = "ROR ${rd.code}, ${rs.code}"
}
