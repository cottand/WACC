@file:Suppress("ClassName")

package ic.org.arm

/* TODO check the values are within bounds (see given javadocs) */

interface SimpleImmedate : Printable

/**
 * 10-bit constant formed by left-shifting an 8-bit value by 2 bits
 */
data class Immed_8_4(val v: Byte) : SimpleImmedate {
  override val code = TODO()
}

/**
 * 8-bit constant
 */
data class Immed_8(val v: Byte) : SimpleImmedate {
  override val code = v.toString()
}

/**
 * 5-bit constant
 */
data class Immed_5(val v: Byte) : SimpleImmedate {
  override val code = v.toString()
}

/**
 * 32-bit constant formed by right-rotating an 8-bit value by an even number of bits
 */
data class Immed_8r(val v: Byte, val r: Byte) : SimpleImmedate {
  override val code = TODO()
}

/**
 * 12-bit constant
 */
data class Immed_12(val v: Int) : SimpleImmedate {
  override val code = v.toString()
}
