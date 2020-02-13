package ic.org.arm

import arrow.core.None
import arrow.core.Option

data class BInstr(override val cond: Option<CondFlag> = None, val label: Label) : ARMCondInstr() {
  constructor(label: Label) : this(None, label)
  override val code = "${opcode("B")} ${label.name}"
}
data class BLInstr(override val cond: Option<CondFlag> = None, val label: Label) : ARMCondInstr() {
  constructor(label: Label) : this(None, label)
  override val code = "${opcode("BL")} ${label.name}"
}
