package ic.org.grammar

import org.intellij.lang.annotations.Identifier

data class Prog(
  val funcs: List<Func>,
  val firstStat: Stat
)

data class Func(
  val retType: Type,
  val ident: Ident,
  val params: List<Param>,
  val stat: Stat
)

sealed class Type
sealed class BaseT : Type()
object IntT : BaseT()
object BoolT : BaseT()
object CharT : BaseT()
object StringT : BaseT()

object ArrayT : Type()
object PairT : Type()

sealed class Stat
object Skip : Stat()
data class Decl(val type: Type, val id: Ident, val rhs: AssRHS) : Stat()
data class Assign(val lhs: AssLHS, val rhs: AssRHS) : Stat()
data class Read(val lhs: AssLHS) : Stat()
data class Free(val expr: Expr) : Stat()
data class Return(val expr: Expr) : Stat()
data class Exit(val expr: Expr) : Stat()
data class Print(val expr: Expr) : Stat()
data class Println(val expr: Expr) : Stat()
data class If(val cond: Expr, val then: Stat, val `else`: Stat) : Stat()
data class While(val cond: Expr, val stat: Stat) : Stat()
data class BegEnd(val stat: Expr) : Stat()
data class StatChain(val stat1: Stat, val stat2: Stat) : Stat()

sealed class AssRHS
data class ArrayLit(val exprs: List<Expr>) : AssRHS()
data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS()
data class Call(val id: Ident, val args: ArgList) : AssRHS()

sealed class Expr : AssRHS()

// TODO try something cleaner
interface PairElem
sealed class PairElemRHS() : PairElem, AssRHS()
sealed class PairElemLHS() : PairElem, AssLHS()

data class Fst(val expr: Expr) : PairElem
data class Snd(val expr: Expr) : PairElem

sealed class AssLHS
data class Ident(val name: String) : AssLHS()

sealed class ArgList

data class Param(val type: Type, val ident: Ident)
