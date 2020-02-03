package ic.org.grammar

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import ic.org.Position
import org.antlr.v4.runtime.tree.TerminalNode

// <program>
data class Prog(
  val funcs: List<Func>,
  val firstStat: Stat
)

// <func>
data class Func(
  val retType: Type,
  val ident: Ident,
  val params: List<Param>,
  val stat: Stat
)

// <param>
data class Param(val type: Type, val ident: Ident)

// <stat>
sealed class Stat {
  abstract val scope: Scope
}

data class Skip(override val scope: Scope) : Stat()
data class Decl(val variable: Variable, val rhs: AssRHS, override val scope: Scope) :
  Stat() {
  val type = variable.type
  val ident = variable.ident
}

data class Assign(val lhs: AssLHS, val rhs: AssRHS, override val scope: Scope) : Stat()
data class Read(val lhs: AssLHS, override val scope: Scope) : Stat()
data class Free(val expr: Expr, override val scope: Scope) : Stat()
data class Return(val expr: Expr, override val scope: Scope) : Stat()
data class Exit(val expr: Expr, override val scope: Scope) : Stat()
data class Print(val expr: Expr, override val scope: Scope) : Stat()
data class Println(val expr: Expr, override val scope: Scope) : Stat()
data class If(val cond: Expr, val then: Stat, val `else`: Stat, override val scope: Scope) : Stat()
data class While(val cond: Expr, val stat: Stat, override val scope: Scope) : Stat()
data class BegEnd(val stat: Stat, override val scope: Scope) : Stat()
data class StatChain(val stat1: Stat, val stat2: Stat, override val scope: Scope) : Stat() {
  val thisStat = stat1
  val nextStat = stat2
}

// <assign-lhs>
sealed class AssLHS {
  abstract val type: Type
}

data class IdentLHS(val variable: Variable) : AssLHS() {
  override val type = variable.type
}

data class ArrayElemLHS(val arrayElem: ArrayElem, val variable: Variable) : AssLHS() {
  override val type = variable.type
}

data class PairElemLHS(val pairElem: PairElem, val variable: Variable, val pairs: PairT) :
  AssLHS() {
  override val type = when (pairElem) {
    is Fst -> pairs.fstT
    is Snd -> pairs.sndT
  }
}

// <assign-rhs>
sealed class AssRHS {
  //abstract fun fetchType(scope: Scope): Option<Type>
  abstract val type: Type
}

data class ExprRHS(val expr: Expr) : AssRHS() {
  override val type = expr.type
}

/**
 * If the array literal is empty, [contentType] is [EmptyArrayT]
 * Caller should verify content.
 */
data class ArrayLit(val exprs: List<Expr>, val arrT: AnyArrayT) : AssRHS() {
  override val type = arrT
}

data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS() {
  override val type = PairT(expr1.type, expr2.type)
}

data class PairElemRHS(val pairElem: PairElem, val pairs: PairT) : AssRHS() {
  override val type = when (pairElem) {
    is Fst -> pairs.fstT
    is Snd -> pairs.sndT
  }
}

data class Call(val func: Func, val args: List<Expr>) : AssRHS() {
  override val type = func.retType
  val name = func.ident
}

// <pair-elem>
sealed class PairElem {
  abstract val expr: Expr
  abstract fun fetchType(scope: Scope): Option<Type>
}

data class Fst(override val expr: Expr) : PairElem() {
  override fun fetchType(scope: Scope): Option<Type> {
    val type = expr.type
    return if (type is PairT) {
      Some(type.fstT)
    } else {
      None
    }
  }
}

data class Snd(override val expr: Expr) : PairElem() {
  override fun fetchType(scope: Scope): Option<Type> {
    val type = expr.type
    return if (type is PairT) {
      Some(type.sndT)
    } else {
      None
    }
  }
}

// <type>
sealed class Type

sealed class BaseT : Type()
open class AnyArrayT : Type() {
  // fun isAlsoArray(other: Type) = other is AnyArrayT
}

// Empty array e.g. []
class EmptyArrayT : AnyArrayT() {
  override fun equals(other: Any?): Boolean = other is EmptyArrayT

  override fun hashCode(): Int {
    return javaClass.hashCode()
  }
}

// TODO Write more documentation comments for types
/**
 * [depth] = 1 for 1-dimensional Array
 */
data class ArrayT private constructor(val type: Type, val depth: Int = 1) : AnyArrayT() {
  init {
    require(depth > 0)
  }

  companion object {
    /**
     * Creates an [ArrayT] of base type [type] with depth [depth]
     */
    fun make(type: Type, depth: Int): ArrayT {
      val deepest = ArrayT(type, 1)
      tailrec fun helper(remainingDepth: Int, prev: ArrayT): ArrayT = when (remainingDepth) {
        depth -> prev
        else -> helper(remainingDepth - 1, ArrayT(prev, remainingDepth))
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
  val containsAnys = this is PairT && (fstT == AnyArrayT() || sndT == AnyArrayT())
  override fun equals(other: Any?): Boolean = other is AnyPairTs && other !is PairT
  override fun hashCode(): Int = 21353
}
data class PairT(val fstT: Type, val sndT: Type) : AnyPairTs()

// <base-type>
object IntT : BaseT()

object BoolT : BaseT()
object CharT : BaseT()
object StringT : BaseT()

// <ident>
data class Ident(val name: String) {
  constructor(node: TerminalNode) : this(node.text)
}

// <array-elem>
data class ArrayElem(
  val ident: Ident,
  val indices: List<Expr>
)
