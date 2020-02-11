package ic.org.arm

sealed class AddrMode2P : Printable

data class ImmOffsetAddrMode2P(val rn: Reg, val sign: Sign, val imm: Immed_12) : AddrMode2P() {
  override val code = "[${rn.code}], #${sign.code}${imm.code}"
}
data class ZeroOffsetAddrMode2P(val rn: Reg) : AddrMode2P() {
  override val code = "[${rn.code}]"
}
data class RegOffsetAddrMode2P(val rn: Reg, val sign: Sign, val rm: Reg) : AddrMode2P() {
  override val code = "[${rn.code}], ${sign.code}${rm.code}"
}
