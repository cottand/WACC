package ic.org.arm

sealed class AddrMode2 : Printable

/* Normal Offset */

data class ImmOffsetAddrMode2(val rn: Register, val imm: Immed_12) : AddrMode2() {
  override val code = "[${rn.code}, #+${imm.code}]"
}

/**
 * 32 bit constant
 * TODO is this part of AddrMode2??
 *
 * see [StackOverflow](https://reverseengineering.stackexchange.com/questions/17666/how-does-the-ldr-instruction-work-on-arm)
 */
data class ImmEquals(val v32bit: Int) : AddrMode2() {
  override val code = "=$v32bit"
}

data class ZeroOffsetAddrMode2(val rn: Register) : AddrMode2() {
  override val code = "[${rn.code}]"
}

data class RegOffsetAddrMode2(val rn: Register, val sign: Sign, val rm: Register) : AddrMode2() {
  override val code = "[${rn.code}, ${sign.code}${rm.code}]"
}

/* Pre-indexed Offset */

data class ImmPreOffsetAddrMode2(val rn: Register, val imm: Immed_12) : AddrMode2() {
  override val code = "[${rn.code}], #+${imm.code}"
}

data class ZeroPreOffsetAddrMode2(val rn: Register) : AddrMode2() {
  override val code = "[${rn.code}]"
}

data class RegPreOffsetAddrMode2(val rn: Register, val sign: Sign, val rm: Register) : AddrMode2() {
  override val code = "[${rn.code}, ${sign.code}${rm.code}]!"
}

/* Post-indexed Offset */

data class ImmPostOffsetAddrMode2(val rn: Register, val sign: Sign, val imm: Immed_12) : AddrMode2() {
  override val code = "[${rn.code}], #${sign.code}${imm.code}"
}

data class ZeroPostOffsetAddrMode2(val rn: Register) : AddrMode2() {
  override val code = "[${rn.code}]"
}

data class RegPostOffsetAddrMode2(val rn: Register, val sign: Sign, val rm: Register) : AddrMode2() {
  override val code = "[${rn.code}], ${sign.code}${rm.code}"
}
