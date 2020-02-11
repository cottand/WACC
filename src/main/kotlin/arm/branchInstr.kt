package ic.org.arm

import arrow.core.Option

data class BInstr(override val cond: Option<CondFlag>, val label: Label) : ARMInstr(cond) {
  override val code = "B$condStr ${label.code}"
}
data class BLInstr(override val cond: Option<CondFlag>, val label: Label) : ARMInstr(cond) {
  override val code = "BL$condStr ${label.code}"
}
