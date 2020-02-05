package ic.org.ast

import ic.org.util.Position
import org.antlr.v4.runtime.tree.TerminalNode

// <program>
data class Prog(
  val funcs: List<Func>
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
  abstract val pos: Position
}

data class Skip(override val scope: Scope, override val pos: Position) : Stat()
data class Decl(val variable: Variable, val rhs: AssRHS, override val scope: Scope, override val pos: Position) :
  Stat() {
  val type = variable.type
  val ident = variable.ident
}

data class Assign(val lhs: AssLHS, val rhs: AssRHS, override val scope: Scope, override val pos: Position) : Stat()
data class Read(val lhs: AssLHS, override val scope: Scope, override val pos: Position) : Stat()
data class Free(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat()
data class Return(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat()
data class Exit(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat()
data class Print(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat()
data class Println(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat()
data class If(val cond: Expr, val then: Stat, val `else`: Stat, override val scope: Scope, override val pos: Position) :
  Stat()

data class While(val cond: Expr, val stat: Stat, override val scope: Scope, override val pos: Position) : Stat()
data class BegEnd(val stat: Stat, override val scope: Scope, override val pos: Position) : Stat()
data class StatChain(val stat1: Stat, val stat2: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  val thisStat = stat1
  val nextStat = stat2
}

// <pair-elem>
sealed class PairElem {
  abstract val expr: Expr

  companion object {
    fun fst(expr: Expr) = Fst(expr)
    fun snd(expr: Expr) = Snd(expr)
  }
}

data class Fst internal constructor(override val expr: Expr) : PairElem()

data class Snd internal constructor(override val expr: Expr) : PairElem()

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class Ident(val name: String) {
  constructor(node: TerminalNode) : this(node.text)

  override fun toString() = name
}
