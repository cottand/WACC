package ic.org.arm

import arrow.core.Option

data class LDRInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2) : ARMCondInstr(cond) {
  override val code = "LDR$condStr ${rd.code}, ${addressing.code}"
}
data class LDRTInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2P) : ARMCondInstr(cond) {
  override val code = "LDR${condStr}T ${rd.code}, ${addressing.code}"
}
data class LDRPCInstr(override val cond: Option<CondFlag>, val addressing: AddrMode2P) : ARMCondInstr(cond) {
  // TODO should r15 be written as "pc"?
  override val code = "LDR${condStr}T r15 ${addressing.code}"
}
data class LDRBInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2) : ARMCondInstr(cond) {
  override val code = "LDR${condStr}B ${rd.code}, ${addressing.code}"
}
data class LDRBTInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2P) : ARMCondInstr(cond) {
  override val code = "LDR${condStr}BT ${rd.code}, ${addressing.code}"
}
data class LDRSBInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode3) : ARMCondInstr(cond) {
  override val code = "LDR${condStr}SB ${rd.code}, ${addressing.code}"
}
data class LDRHInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode3) : ARMCondInstr(cond) {
  override val code = "LDR${condStr}H ${rd.code}, ${addressing.code}"
}
data class LDRSHInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode3) : ARMCondInstr(cond) {
  override val code = "LDR${condStr}SH ${rd.code}, ${addressing.code}"
}
data class LDRDInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode3) : ARMCondInstr(cond) {
  override val code = "LDR${condStr}D ${rd.code}, ${addressing.code}"
}
