package ic.org.arm

/**
 * Operand2 types
 */
sealed class Operand2 : Printable

data class ImmOperand2(val imm: SimpleImmedate) : Operand2() {
  override val code = "#${imm.code}"
}
data class LSLImmOperand2(val rm: Register, val imm: Immed_5) : Operand2() {
  override val code = "${rm.code} LSL #${imm.code}"
}
data class LSRImmOperand2(val rm: Register, val imm: Immed_5) : Operand2() {
  override val code = "${rm.code} LSR #${imm.code}"
}
data class ASRImmOperand2(val rm: Register, val imm: Immed_5) : Operand2() {
  override val code = "${rm.code} ASR #${imm.code}"
}
data class RORImmOperand2(val rm: Register, val imm: Immed_5) : Operand2() {
  override val code = "${rm.code} ROR #${imm.code}"
}
data class RegOperand2(val rm: Register) : Operand2() {
  override val code = rm.code
}
data class LSLRegOperand2(val rm: Register, val rs: Register) : Operand2() {
  override val code = "${rm.code} LSL ${rs.code}"
}
data class LSRRegOperand2(val rm: Register, val rs: Register) : Operand2() {
  override val code = "${rm.code} LSR ${rs.code}"
}
data class ASRRegOperand2(val rm: Register, val rs: Register) : Operand2() {
  override val code = "${rm.code} ASR ${rs.code}"
}
data class RORRegOperand2(val rm: Register, val rs: Register) : Operand2() {
  override val code = "${rm.code} ROR ${rs.code}"
}
data class RRXOperand2(val rm: Register) : Operand2() {
  override val code = "${rm.code} RRX"
}


