package ic.org.arm.instr

import ic.org.arm.ARMInstr
import ic.org.arm.Immed_5
import ic.org.arm.Register

data class LSLImmInstr(val rd: Register, val rm: Register, val imm: Immed_5) : ARMInstr() {
  override val code = "LSL ${rd.code}, ${rm.code}, #${imm.code}"
}

data class LSLRegInstr(val rd: Register, val rs: Register) : ARMInstr() {
  override val code = "LSL ${rd.code}, ${rs.code}"
}

data class LSRImmInstr(val rd: Register, val rm: Register, val imm: Immed_5) : ARMInstr() {
  override val code = "LSR ${rd.code}, ${rm.code}, #${imm.code}"
}

data class LSRRegInstr(val rd: Register, val rs: Register) : ARMInstr() {
  override val code = "LSR ${rd.code}, ${rs.code}"
}

data class ASRImmInstr(val rd: Register, val rm: Register, val imm: Immed_5) : ARMInstr() {
  override val code = "ASR ${rd.code}, ${rm.code}, #${imm.code}"
}

data class ASRRegInstr(val rd: Register, val rs: Register) : ARMInstr() {
  override val code = "ASR ${rd.code}, ${rs.code}"
}

data class RORInstr(val rd: Register, val rs: Register) : ARMInstr() {
  override val code = "ROR ${rd.code}, ${rs.code}"
}
