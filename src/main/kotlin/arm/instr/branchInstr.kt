package ic.org.arm.instr

import arrow.core.None
import arrow.core.some
import ic.org.arm.ARMCondInstr
import ic.org.arm.CondFlag
import ic.org.arm.Flag
import ic.org.arm.AsmLabel

data class BInstr(override val cond: Flag = None, val label: AsmLabel) : ARMCondInstr() {
  constructor(label: AsmLabel) : this(None, label)

  override val code = "${opcode("B")} ${label.name}"
}

data class BLInstr(override val cond: Flag = None, val label: AsmLabel) : ARMCondInstr() {
  constructor(label: AsmLabel) : this(None, label)
  constructor(labelName: String) : this(None, AsmLabel(labelName))
  constructor(condFlag: CondFlag, label: AsmLabel) : this(condFlag.some(), label)

  override val code = "${opcode("BL")} ${label.name}"
}
