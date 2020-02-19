package ic.org.arm

import arrow.core.None
import arrow.core.some

data class BInstr(override val cond: Flag = None, val label: Label) : ARMCondInstr() {
  constructor(label: Label) : this(None, label)
  override val code = "${opcode("B")} ${label.name}"
}
data class BLInstr(override val cond: Flag = None, val label: Label) : ARMCondInstr() {
  constructor(label: Label) : this(None, label)
  constructor(labelName: String) : this(None, Label(labelName))
  constructor(condFlag: CondFlag, label: Label) : this(condFlag.some(), label)
  override val code = "${opcode("BL")} ${label.name}"
}
