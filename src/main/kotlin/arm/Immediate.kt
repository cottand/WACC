@file:Suppress("ClassName")

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
  override val code = v.toString()
}

/**
 * 5-bit constant
 */
data class Immed_5(val v: Byte) : Printable {
  override val code = v.toString()
}

/**
 * 32-bit constant formed by right-rotating an 8-bit value by an even number of bits
 */
data class Immed_8r(val v: Byte, val r: Byte) : Printable {
  override val code = if (r != 0.toByte()) TODO("Implement rotation") else v.toString()
}

/**
 * 12-bit constant
 */
data class Immed_12(val v: Int) : Printable {
  override val code = v.toString()
}
