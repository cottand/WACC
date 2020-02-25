package ic.org.arm.instr

import arrow.core.None
import ic.org.arm.ARMCondInstr
import ic.org.arm.Flag
import ic.org.arm.Register
import ic.org.arm.addressing.AddrMode2
import ic.org.arm.addressing.AddrMode2P
import ic.org.arm.addressing.AddrMode3

data class STRInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  constructor(rd: Register, addressing: AddrMode2) : this(None, rd, addressing)

  override val code = "${opcode("STR")} ${rd.code}, ${addressing.code}"
}

data class STRTInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2P) : ARMCondInstr() {
  override val code = "${opcode("STR")}T ${rd.code}, ${addressing.code}"
}

data class STRBInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  constructor(rd: Register, addressing: AddrMode2) : this(None, rd, addressing)

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
