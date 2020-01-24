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

data class Param(val type: Type, val ident: Ident)

data class Ident(val name: String)

sealed class Expr
data class IntExpr(val value: Int) : Expr()
data class BoolExpr(val value: Boolean) : Expr()
data class CharExpr(val value: Char) : Expr()
data class StrExpr(val value: String) : Expr()
object NullPairExpr : Expr()
data class IdentExpr(val ident: Ident) : Expr()
data class ArrayElemExpr(
  val ident: Ident,
  val indices: List<Expr>
) : Expr()
data class UnaryOperExpr(
  val unaryOper: UnaryOper,
  val expr: Expr
) : Expr()
data class BinaryOperExpr(
  val expr1: Expr,
  val binaryOper: BinaryOper,
  val expr2: Expr
) : Expr()

sealed class UnaryOper
object NotUO : UnaryOper()
object MinusUO : UnaryOper()
object LenUO : UnaryOper()
object OrdUO : UnaryOper()
object ChrUO : UnaryOper()

sealed class BinaryOper
object TimesBO : BinaryOper()
object DivisionBO : BinaryOper()
object ModBO : BinaryOper()
object PlusBO : BinaryOper()
object MinusBO : BinaryOper()
object GtBO : BinaryOper()
object GeqBO : BinaryOper()
object LtBO : BinaryOper()
object LeqBO : BinaryOper()
object EqBO : BinaryOper()
object NeqBO : BinaryOper()
object AndBO : BinaryOper()
object OrBO : BinaryOper()
