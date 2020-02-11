package ic.org.arm

import arrow.core.Option

data class BInstr(override val cond: Option<CondFlag>, val label: Label) : ARMCondInstr() {
  override val code = "${opcode("B")} ${label.code}"
}
data class BLInstr(override val cond: Option<CondFlag>, val label: Label) : ARMCondInstr() {
  override val code = "${opcode("BL")} ${label.code}"
}
