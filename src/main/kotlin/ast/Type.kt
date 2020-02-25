package ic.org.ast

import ic.org.arm.*
import ic.org.arm.addressing.AddrMode2
import ic.org.arm.instr.LDRBInstr
import ic.org.arm.instr.LDRInstr
import ic.org.arm.instr.STRBInstr
import ic.org.arm.instr.STRInstr

/**
 * Class representing one of WACC's types.
 */
sealed class Type {
  fun sizedSTR(rd: Register, addr: AddrMode2) = when (size) {
    Sizes.Word -> STRInstr(rd, addr)
    Sizes.Char -> STRBInstr(rd, addr)
  }

  fun sizedLDR(rd: Register, addr: AddrMode2) = when (size) {
    Sizes.Word -> LDRInstr(rd, addr)
    Sizes.Char -> LDRBInstr(rd, addr)
  }

  /**
   * Determines whether a variable of type `this `would accept a variable of type [other].
   * For example, [AnyArrayT] matches with [ArrayT].
   */
  abstract fun matches(other: Type): Boolean

  abstract val size: Sizes

  enum class Sizes(val bytes: Int) {
    Word(4),
    Char(1)
  }
}

val Type.Sizes.Pointer
  get() = Type.Sizes.Word

sealed class BaseT : Type() {
  override fun matches(other: Type) = this == other
}

open class AnyArrayT : Type() {
  override fun matches(other: Type) = other is EmptyArrayT
  override val size = Sizes.Word

  override fun toString(): String = "AnyArray"
}

// Empty array e.g. []
class EmptyArrayT : AnyArrayT() {
  override fun equals(other: Any?): Boolean = other is EmptyArrayT
  override fun hashCode() = 237371
}

/**
 * [depth] = 1 for 1-dimensional Array
 */
@Suppress("DataClassPrivateConstructor")
data class ArrayT private constructor(val type: Type) : AnyArrayT() {

  val depth: Int = if (type is ArrayT) type.depth + 1 else 1

  override fun matches(other: Type) =
    other is EmptyArrayT ||
      (other is ArrayT && other.depth == depth && nestedType.matches(other.nestedType))

  override fun toString() =
    nestedType.toString() + (0 until depth).joinToString(separator = "") { "[]" }

  companion object {
    /**
     * Creates an [ArrayT] of base type [type] with depth [depth]
     */
    fun make(type: Type, depth: Int = 1): ArrayT {
      val deepest = ArrayT(type)
      tailrec fun helper(remainingDepth: Int, prev: ArrayT): ArrayT = when (remainingDepth) {
        1 -> prev
        else -> helper(remainingDepth - 1, ArrayT(prev))
      }
      return helper(depth, deepest)
    }
  }

  val nestedType: Type
   get() = when (type) {
      is ArrayT -> type.nestedType
      else -> type
    }

  fun nthNestedType(ndepth: Int): Type {
    require(ndepth in 0..this.depth)
    return when {
      ndepth == 1 -> type
      type is ArrayT -> type.nthNestedType(ndepth - 1)
      else -> throw IllegalArgumentException("Bad depth: $ndepth, not in $depth for type $type")
    }
  }
}

open class AnyPairTs : Type() {
  override fun equals(other: Any?): Boolean = other is AnyPairTs && other !is PairT
  override fun hashCode(): Int = 21353
  override fun toString() = "AnyPair"

  override fun matches(other: Type) = other is AnyPairTs
  override val size = Sizes.Word
}

data class PairT(val fstT: Type, val sndT: Type) : AnyPairTs() {
  override fun toString() = "Pair($fstT, $sndT)"
  override fun matches(other: Type) =
    other is PairT && fstT.matches(other.fstT) && sndT.matches(other.sndT) ||
      other == AnyPairTs()
}

// <base-type>
object IntT : BaseT() {
  override fun toString() = "Int"
  override val size = Sizes.Word
}

object BoolT : BaseT() {
  override fun toString() = "Bool"
  override val size = Sizes.Char
}

object CharT : BaseT() {
  override fun toString() = "Char"
  override val size = Sizes.Char
}

object StringT : BaseT() {
  override fun toString() = "String"
  override val size = Sizes.Word
}
