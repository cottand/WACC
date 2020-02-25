package ic.org.arm.instr

import arrow.core.None
import arrow.core.some
import ic.org.arm.ARMCondInstr
import ic.org.arm.CondFlag
import ic.org.arm.Flag
import ic.org.arm.Register
import ic.org.arm.addressing.AddrMode2
import ic.org.arm.addressing.AddrMode2P
import ic.org.arm.addressing.AddrMode3
import ic.org.arm.addressing.ImmEquals32b

data class LDRInstr(override val cond: Flag = None, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  constructor(condFlag: CondFlag, rd: Register, addressing: AddrMode2) : this(condFlag.some(), rd, addressing)
  constructor(rd: Register, addressing: AddrMode2) : this(None, rd = rd, addressing = addressing)
  constructor(rd: Register, int32b: Int) : this(
    None, rd = rd, addressing = ImmEquals32b(
      int32b
    )
  )

  override val code = "${opcode("LDR")} ${rd.code}, ${addressing.code}"
}

data class LDRBInstr(override val cond: Flag, val rd: Register, val addressing: AddrMode2) : ARMCondInstr() {
  constructor(rd: Register, addressing: AddrMode2) : this(None, rd = rd, addressing = addressing)
  constructor(rd: Register, int32b: Int) : this(None, rd = rd, addressing = ImmEquals32b(int32b))

  override val code = "${opcode("LDR")}B ${rd.code}, ${addressing.code}"
}
