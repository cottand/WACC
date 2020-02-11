package ic.org.arm

/* TODO check the values are within bounds (see given javadocs) */

/**
 * 10-bit constant formed by left-shifting an 8-bit value by 2 bits
 */
data class Immed_8_4(val v: Byte) : Printable {
  override val code = TODO()
}

/**
 * 8-bit constant
 */
data class Immed_8(val v: Byte) : Printable {
  override val code = "#$v"
}

/**
 * 5-bit constant
 */
data class Immed_5(val v: Byte) : Printable {
  override val code = "#$v"
}

/**
 * 32-bit constant formed by right-rotating an 8-bit value by an even number of bits
 */
data class Immed_8r(val v: Byte, val r: Byte) : Printable {
  override val code = TODO()
}
