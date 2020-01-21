package ic.org.grammar

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

data class Ident(val name: String)
data class Param(val type: Type, val ident: Ident)
