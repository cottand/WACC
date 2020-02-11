package ic.org.arm

import arrow.core.Option

data class STRInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2) : ARMCondInstr(cond) {
  override val code = "STR$condStr ${rd.code}, ${addressing.code}"
}
data class STRTInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2P) : ARMCondInstr(cond) {
  override val code = "STR${condStr}T ${rd.code}, ${addressing.code}"
}
data class STRBInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2) : ARMCondInstr(cond) {
  override val code = "STR${condStr}B ${rd.code}, ${addressing.code}"
}
data class STRBTInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode2P) : ARMCondInstr(cond) {
  override val code = "STR${condStr}BT ${rd.code}, ${addressing.code}"
}
data class STRHInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode3) : ARMCondInstr(cond) {
  override val code = "STR${condStr}H ${rd.code}, ${addressing.code}"
}
data class STRDInstr(override val cond: Option<CondFlag>, val rd: Reg, val addressing: AddrMode3) : ARMCondInstr(cond) {
  override val code = "STR${condStr}D ${rd.code}, ${addressing.code}"
}
