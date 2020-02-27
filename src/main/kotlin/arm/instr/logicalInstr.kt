package ic.org.arm.instr

import arrow.core.None
import arrow.core.Some
import ic.org.arm.*

data class MOVInstr(
  override val cond: Flag,
  override val s: Boolean,
  val rd: Register,
  val op2: Operand2
) : ARMCondSInstr() {
  constructor(cond: Flag = None, s: Boolean = false, rd: Register, op2: Register) : this(
    cond, s, rd,
    RegOperand2(op2)
  )

  constructor(rd: Register, op2: Register) : this(None, false, rd, RegOperand2(op2))

  constructor(cond: CondFlag, s: Boolean = false, rd: Register, op2: Operand2) : this(Some(cond), s, rd, op2)
  constructor(cond: CondFlag, s: Boolean = false, rd: Register, imm8b: Byte)
    : this(cond, s, rd, ImmOperand2(Immed_8r_bs(imm8b, 0)))

  constructor(rd: Register, char: Char) : this(None, false, rd, ImmOperand2(Immed_8r_char(char)))
  constructor(rd: Register, imm8b: Byte) : this(None, false, rd, ImmOperand2(Immed_8r_bs(imm8b, 0)))

  override val code = "${opcode("MOV")} ${rd.code}, ${op2.code}"
}

data class ANDInstr(
  override val cond: Flag,
  override val s: Boolean,
  val rd: Register,
  val rn: Register,
  val op2: Operand2
) : ARMCondSInstr() {
  override val code = "${opcode("AND")} ${rd.code}, ${rn.code}, ${op2.code}"
}

data class EORInstr(
  override val cond: Flag,
  override val s: Boolean,
  val rd: Register,
  val rn: Register,
  val op2: Operand2
) : ARMCondSInstr() {
  override val code = "${opcode("EOR")} ${rd.code}, ${rn.code}, ${op2.code}"
}

data class ORRInstr(
  override val cond: Flag,
  override val s: Boolean,
  val rd: Register,
  val rn: Register,
  val op2: Operand2
) : ARMCondSInstr() {
  override val code = "${opcode("ORR")} ${rd.code}, ${rn.code}, ${op2.code}"
}
