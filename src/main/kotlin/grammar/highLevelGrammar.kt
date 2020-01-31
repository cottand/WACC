package ic.org.grammar

import arrow.core.None
import arrow.core.Option
import ic.org.Position
import arrow.core.Some
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
  val position: Option<Position> = Option.empty()
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
  abstract fun fetchType(scope: Scope): Option<Type>
}

data class IdentLHS(val ident: Ident) : AssLHS() {
  override fun fetchType(scope: Scope): Option<Type> = scope[ident].map { it.type }
}

data class ArrayElemLHS(val arrayElem: ArrayElem) : AssLHS() {
  override fun fetchType(scope: Scope): Option<Type> = scope[arrayElem.ident].map { it.type }
}

data class PairElemLHS(val pairElem: PairElem) : AssLHS() {
  override fun fetchType(scope: Scope): Option<Type> = Some(pairElem.expr.type)
}

// <assign-rhs>
sealed class AssRHS {
  abstract fun fetchType(scope: Scope): Option<Type>
}

data class ExprRHS(val expr: Expr) : AssRHS() {
  override fun fetchType(scope: Scope): Option<Type> = Some(expr.type)
}

data class ArrayLit(val exprs: List<Expr>) : AssRHS() {
  override fun fetchType(scope: Scope): Option<Type> {
    if (exprs.isEmpty()) {
      // TODO should this be type empty array somehow?
      return None
    }

    // Make sure expressions all have the same type
    // We assert since it should always be the case
    val t = exprs[0].type
    for (e in exprs) {
      assert(e.type != t)
    }

    return Some(t)
  }
}

data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS() {
  override fun fetchType(scope: Scope): Option<Type> = Some(PairT(expr1.type, expr2.type))
}

data class PairElemRHS(val pairElem: PairElem) : AssRHS() {
  override fun fetchType(scope: Scope): Option<Type> = Some(pairElem.expr.type)
}

data class Call(val id: Ident, val args: List<Expr>) : AssRHS() {
  override fun fetchType(scope: Scope): Option<Type> = scope[id].map { it.type }
}

// <pair-elem>
sealed class PairElem {
  abstract val expr: Expr
}

data class Fst(override val expr: Expr) : PairElem()
data class Snd(override val expr: Expr) : PairElem()

// <type>
sealed class Type

sealed class BaseT : Type()
open class AnyArrayT : Type() {
  //fun isAlsoArray(other: Type) = other is AnyArrayT
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
    fun make(type: Type, depth: Int) : ArrayT {
      val deepest = ArrayT(type, 1)
      tailrec fun helper(remainingDepth: Int, prev: ArrayT) : ArrayT = when(remainingDepth) {
        depth -> prev
        else -> helper(remainingDepth -1, ArrayT(prev, remainingDepth))
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

open class AnyPairTs : Type()
data class PairT(val fstT: Type, val sndT: Type) : AnyPairTs()
object NDPairT : AnyPairTs()

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
