package ic.org.grammar

import kotlinx.collections.immutable.PersistentList

data class Ident(val name: String)
data class Param(val type: Type, val ident: Ident)

data class Prog(
  val retType: Type,
  val ident: Ident,
  val params: PersistentList<Param>,
  val stat: Stat
)

sealed class Type
sealed class BaseT : Type()
object StringT : BaseT()
object CharT : BaseT()
object BoolT : BaseT()
object IntT : BaseT()

object ArrayT : Type()
object PairT : Type()

sealed class Stat


