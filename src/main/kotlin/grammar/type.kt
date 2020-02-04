package ic.org.grammar

import org.antlr.v4.runtime.tree.TerminalNode

// <type>
sealed class Type {
  abstract fun matches(other: Type): Boolean
}

sealed class BaseT : Type() {
  override fun matches(other: Type) = this == other
}

open class AnyArrayT : Type() {
  override fun matches(other: Type) = other is EmptyArrayT
}

// Empty array e.g. []
class EmptyArrayT : AnyArrayT() {
  override fun equals(other: Any?): Boolean = other is EmptyArrayT

  override fun hashCode() = 237371
}

// TODO Write more documentation comments for types
/**
 * [depth] = 1 for 1-dimensional Array
 */
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
      depth == 1 -> type
      type is ArrayT -> type.nthNestedType(depth - 1)
      else -> throw IllegalArgumentException("Bad depth: $ndepth, not in $depth for type $type")
    }
  }
}

open class AnyPairTs : Type() {
  override fun equals(other: Any?): Boolean = other is AnyPairTs && other !is PairT
  override fun hashCode(): Int = 21353
  override fun toString() = "AnyPair"

  override fun matches(other: Type) = other is AnyPairTs
}

data class PairT(val fstT: Type, val sndT: Type) : AnyPairTs() {
  override fun toString() = "Pair($fstT, $sndT)"
  override fun matches(other: Type) =
    other is PairT && fstT.matches(other.fstT) && sndT.matches(other.sndT)
      || other == AnyPairTs()
}

// <base-type>
object IntT : BaseT() {
  override fun toString() = "Int"
}

object BoolT : BaseT() {
  override fun toString() = "Bool"
}

object CharT : BaseT() {
  override fun toString() = "Char"
}

object StringT : BaseT() {
  override fun toString() = "String"
}
