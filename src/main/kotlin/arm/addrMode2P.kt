package ic.org.arm

sealed class AddrMode2P : Printable

data class ImmOffsetAddrMode2P(val rn: Register, val sign: Sign, val imm: Immed_12) : AddrMode2P() {
  override val code = "[${rn.code}], #${sign.code}${imm.code}"
}
data class ZeroOffsetAddrMode2P(val rn: Register) : AddrMode2P() {
  override val code = "[${rn.code}]"
}
data class RegOffsetAddrMode2P(val rn: Register, val sign: Sign, val rm: Register) : AddrMode2P() {
  override val code = "[${rn.code}], ${sign.code}${rm.code}"
}
