package ic.org.arm

/**
 * Operand2 types
 */
sealed class Operand2 : ARMAsmInstr

data class ImmOperand2(val imm: Immed_8r) : Operand2() {
  override val code = "#${imm.code}"
}

data class LSLImmOperand2(val rm: Register, val imm: Immed_5) : Operand2() {
  override val code = "${rm.code}, LSL #${imm.code}"
}

data class ASRImmOperand2(val rm: Register, val imm: Immed_5) : Operand2() {
  override val code = "${rm.code}, ASR #${imm.code}"
}

data class RegOperand2(val rm: Register) : Operand2() {
  override val code = rm.code
}
