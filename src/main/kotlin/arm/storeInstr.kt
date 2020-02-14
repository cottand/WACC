package ic.org.arm

import arrow.core.Option

data class STRInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  override val code = "${opcode("STR")} ${rd.code}, ${addressing.code}"
}
data class STRTInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2P) : ARMCondInstr() {
  override val code = "${opcode("STR")}T ${rd.code}, ${addressing.code}"
}
data class STRBInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  override val code = "${opcode("STR")}B ${rd.code}, ${addressing.code}"
}
data class STRBTInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2P) : ARMCondInstr() {
  override val code = "${opcode("STR")}BT ${rd.code}, ${addressing.code}"
}
data class STRHInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode3) : ARMCondInstr() {
  override val code = "${opcode("STR")}H ${rd.code}, ${addressing.code}"
}
data class STRDInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode3) : ARMCondInstr() {
  override val code = "${opcode("STR")}D ${rd.code}, ${addressing.code}"
}
