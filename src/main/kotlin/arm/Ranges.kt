@file:Suppress("EnumEntryName", "EXPERIMENTAL_API_USAGE")

package ic.org.arm

import kotlin.math.pow

/**
 * Holds ranges corresponding to the max and min values that number of bits can
 * encode. Used, for example, when parsing wacc's Ints.
 */
enum class Ranges(val range: IntRange, private val n: Int) : ClosedRange<Int> by range {
  _32b(Int.MIN_VALUE..Int.MAX_VALUE, 32),
  _12b(range(12), 12),
  _8b(range(8), 8),
  _5b(range(5), 5);

  val positive by lazy {
    0.toUInt()..(2.toDouble().pow(n).toUInt() - 1.toUInt())
  }
}

/**
 * Returns the max an min values for an integer of [n] bits
 */
private fun range(n: Int) = -(1.shl(n - 1)) until 1.shl(n - 1)
