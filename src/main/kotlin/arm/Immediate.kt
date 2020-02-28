@file:Suppress("ClassName", "EXPERIMENTAL_API_USAGE")

package ic.org.arm

/**
 * 8-bit constant
 */
data class Immed_8(val v: Byte) : Printable {
  override val code = v.toString()
}

/**
 * 5-bit constant
 */
data class Immed_5(val v: UByte) : Printable {
  constructor(v: Int) : this(v.toUByte())

  init {
    require(v.toUInt() in Ranges._5b.positive)
  }

  override val code = v.toString()
}

sealed class Immed_8r : Printable

/**
 * 32-bit constant formed by right-rotating an 8-bit value by an even number of bits
 */
data class Immed_8r_bs(val v: Byte, val r: Byte = 0) : Immed_8r() {
  override val code = if (r != 0.toByte()) throw UnsupportedOperationException() else v.toString()
}

data class Immed_8r_char(val c: Char) : Immed_8r() {
  override val code = "'$c'"
}

/**
 * 12-bit constant
 */
data class Immed_12(val v: Int) : Printable {
  init {
    require(v in Ranges._12b)
  }

  override val code = v.toString()
}
