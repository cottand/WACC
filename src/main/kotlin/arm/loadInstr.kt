package ic.org.arm

import arrow.core.None
import arrow.core.Option

data class LDRInstr(override val cond: Option<CondFlag> = None, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  constructor(rd: Register, addressing: AddrMode2) : this(None, rd = rd, addressing = addressing)
  override val code = "${opcode("LDR")} ${rd.code}, ${addressing.code}"
}
data class LDRTInstr(override val cond: Option<CondFlag>, val rd: Register, val addressing: AddrMode2P) : ARMCondInstr() {
  override val code = "${opcode("LDR")}T ${rd.code}, ${addressing.code}"
}
data class LDRPCInstr(override val cond: Option<CondFlag>, val addressing: AddrMode2P) : ARMCondInstr() {
  // TODO should r15 be written as "pc"?
  override val code = "${opcode("LDR")} r15 ${addressing.code}"
}
data class LDRBInstr(override val cond: Option<CondFlag>, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  override val code = "${opcode("LDR")}B ${rd.code}, ${addressing.code}"
}
data class LDRBTInstr(override val cond: Option<CondFlag>, val rd: Register, val addressing: AddrMode2P) : ARMCondInstr() {
  override val code = "${opcode("LDR")}BT ${rd.code}, ${addressing.code}"
}
data class LDRSBInstr(override val cond: Option<CondFlag>, val rd: Register, val addressing: AddrMode3) : ARMCondInstr() {
  override val code = "${opcode("LDR")}SB ${rd.code}, ${addressing.code}"
}
data class LDRHInstr(override val cond: Option<CondFlag>, val rd: Register, val addressing: AddrMode3) : ARMCondInstr() {
  override val code = "${opcode("LDR")}H ${rd.code}, ${addressing.code}"
}
data class LDRSHInstr(override val cond: Option<CondFlag>, val rd: Register, val addressing: AddrMode3) : ARMCondInstr() {
  override val code = "${opcode("LDR")}SH ${rd.code}, ${addressing.code}"
}
data class LDRDInstr(override val cond: Option<CondFlag>, val rd: Register, val addressing: AddrMode3) : ARMCondInstr() {
  override val code = "${opcode("LDR")}D ${rd.code}, ${addressing.code}"
}
