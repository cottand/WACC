package ic.org.arm.addressing

import ic.org.arm.Immed_8
import ic.org.arm.Printable
import ic.org.arm.Register
import ic.org.arm.Sign

sealed class AddrMode3 : Printable

/* Immediate */

data class ImmOffsetAddrMode3(val rn: Register, val sign: Sign, val imm: Immed_8) : AddrMode3() {
  override val code = "[${rn.code}, #${sign.code}${imm.code}]"
}
data class ImmPreOffsetAddrMode3(val rn: Register, val sign: Sign, val imm: Immed_8) : AddrMode3() {
  override val code = "[${rn.code}, #${sign.code}${imm.code}]!"
}
data class ImmPostOffsetAddrMode3(val rn: Register, val sign: Sign, val imm: Immed_8) : AddrMode3() {
  override val code = "[${rn.code}], #${sign.code}${imm.code}"
}

/* Register */

data class RegOffsetAddrMode3(val rn: Register, val sign: Sign, val rm: Register) : AddrMode3() {
  override val code = "[${rn.code}, ${sign.code}${rm.code}]"
}
data class RegPreOffsetAddrMode3(val rn: Register, val sign: Sign, val rm: Register) : AddrMode3() {
  override val code = "[${rn.code}, ${sign.code}${rm.code}]!"
}
data class RegPostOffsetAddrMode3(val rn: Register, val sign: Sign, val rm: Register) : AddrMode3() {
  override val code = "[${rn.code}], ${sign.code}${rm.code}"
}
