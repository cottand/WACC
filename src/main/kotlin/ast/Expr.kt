package ic.org.ast

import arrow.core.extensions.list.foldable.forAll
import arrow.core.valid
import ic.org.util.*

// <expr>
sealed class Expr {
  abstract val type: Type
  abstract fun code(): Code
}

data class IntLit(val value: Int) : Expr() {
  override val type = IntT
  override fun code(): Code = Code.empty
  override fun toString(): String = value.toString()

  companion object {
    /**
     * Range which a WACC Int can represent. Beacuse its implementation is a 32 bit signed number,
     * it is the same range as the JVM Int primitive.
     */
    val range = Int.MIN_VALUE..Int.MAX_VALUE
  }
}

data class BoolLit(val value: Boolean) : Expr() {
  override val type = BoolT
  override fun toString(): String = value.toString()
  override fun code(): Code = TODO("not implemented")
}

data class CharLit(val value: Char) : Expr() {
  override val type = CharT
  override fun toString(): String = "$value"
  override fun code(): Code = TODO("not implemented")
}

data class StrLit(val value: String) : Expr() {
  override val type = StringT
  override fun toString(): String = value
  override fun code(): Code = TODO("not implemented")
}

object NullPairLit : Expr() {
  override val type = AnyPairTs() // TODO double check
  override fun toString(): String = "null"
  override fun code(): Code = TODO("not implemented")
}

data class IdentExpr(val vari: Variable) : Expr() {
  override val type = vari.type
  override fun toString(): String = vari.ident.name
  override fun code(): Code = TODO("not implemented")
}

data class ArrayElemExpr internal constructor(
  val variable: Variable,
  val exprs: List<Expr>,
  override val type: Type
) : Expr() {
  override fun toString(): String = variable.ident.name + exprs.indices.joinToString(separator = "") { "[]" }
  override fun code(): Code = TODO("not implemented")

  companion object {
    /**
     * Builds a type safe [ArrayElemExpr] from [variable]    */
    fun make(pos: Position, variable: Variable, exprs: List<Expr>): Parsed<ArrayElemExpr> {
      val arrType = variable.type
      return when {
        // Array access indexes must evaluate to ints
        !exprs.forAll { it.type == IntT } -> {
          val badExpr = exprs.find { it.type != IntT }!!
          IllegalArrayAccess(pos, badExpr.toString(), badExpr.type).toInvalidParsed()
        }
        arrType !is ArrayT -> NOT_REACHED()
        exprs.size > arrType.depth ->
          TypeError(pos, AnyArrayT(), arrType, "Array access").toInvalidParsed()
        else -> ArrayElemExpr(
          variable,
          exprs,
          arrType.nthNestedType(exprs.size)
        ).valid()
      }
    }
  }
}

data class UnaryOperExpr(val unaryOper: UnaryOper, val expr: Expr) : Expr() {
  override val type: Type = unaryOper.retType

  override fun toString(): String = "$unaryOper $expr"
  override fun code(): Code = TODO("not implemented")

  companion object {
    /**
     * Factory method for safely building a [UnaryOperExpr] from a [UnaryOper]
     *
     * Makes sure [e] is the [Type] that [unOp] expects and returns a [Parsed]
     * accordingly.
     */
    fun build(e: Expr, unOp: UnaryOper, pos: Position): Parsed<UnaryOperExpr> =
      if (unOp.validArg(e.type))
        UnaryOperExpr(unOp, e).valid()
      else
        TypeError(pos, unOp.argType, e.type, unOp.toString()).toInvalidParsed()
  }
}

data class BinaryOperExpr internal constructor(
  val expr1: Expr,
  val binaryOper: BinaryOper,
  val expr2: Expr
) : Expr() {
  override val type = binaryOper.retType

  override fun toString(): String = "($expr1 $binaryOper $expr2)"
  override fun code(): Code = TODO("not implemented")

  companion object {
    /**
     * Factory method for safely building a [BinaryOperExpr] from a [BinaryOper]
     *
     * Makes sure [e1] and [e2] are the [Type]s that [binOp] expects and returns a [Parsed]
     * accordingly.
     */
    fun build(e1: Expr, binOp: BinaryOper, e2: Expr, pos: Position): Parsed<BinaryOperExpr> {
      val expr = BinaryOperExpr(e1, binOp, e2)
      return if (binOp.validArgs(e1.type, e2.type))
        expr.valid()
      else
        TypeError(pos, binOp.inTypes, e1.type to e2.type, binOp.toString(), expr).toInvalidParsed()
    }
  }
}

// <unary-oper>
/**
 * Represents a unary operator, like `!` or `-`.
 * Type checking is enforced by making each subclass provide typechecking functions.
 * See [LenUO] for why we chose this design
 */
sealed class UnaryOper {
  internal abstract val validArg: (Type) -> Boolean
  internal abstract val resCheck: (Type) -> Boolean
  abstract val argType: Type
  abstract val retType: Type
}

// bool:
object NotUO : UnaryOper() { // !
  override val validArg: (Type) -> Boolean = { it is BoolT }
  override val resCheck: (Type) -> Boolean = { it is BoolT }
  override val argType: Type = BoolT
  override val retType: Type = BoolT
  override fun toString(): String = "!"
}

// int:
object MinusUO : UnaryOper() { // -
  override val validArg: (Type) -> Boolean = { it is IntT }
  override val resCheck: (Type) -> Boolean = { it is IntT }
  override val argType: Type = IntT
  override val retType: Type = IntT
  override fun toString(): String = "-"
}

// arr -> int:
/**
 * The operator `len` is a special case, because it takes any kind of [ArrayT],
 * even though the class [ArrayT] has state depending on the type of the nested arrays. To work
 * around this, we have chosen to use Kotlin `is` clauses, and make a new [Type], [AnyArrayT],
 * which is a supertype of [ArrayT]. This whay, the compile time check
 *
 * `(ArrayT is ArrayT)`
 *
 * will pass for any [ArrayT].
 *
 * Another solution was to just compare [Type] variables (ie, t1 == t2) but this would have
 * involved overriding the JVM's .equals() in a way that would have broken transitivity on
 * equality... So we chose to type a bit more instead.
 * (for example, both a [ArrayT](IntT) and a [ArrayT](CharT) would have been equal to a [AnyArrayT]
 * but not equal between them.
 */
object LenUO : UnaryOper() { // len
  override val validArg: (Type) -> Boolean = { it is ArrayT }
  override val resCheck: (Type) -> Boolean = { it is IntT }
  override val argType: Type = AnyArrayT()
  override val retType: Type = IntT
  override fun toString(): String = "len"
}

// char -> int:
object OrdUO : UnaryOper() {
  override val validArg: (Type) -> Boolean = { it is CharT }
  override val resCheck: (Type) -> Boolean = { it is IntT }
  override val argType: Type = CharT
  override val retType: Type = IntT
  override fun toString(): String = "ord"
}

// int -> char:
object ChrUO : UnaryOper() {
  override val validArg: (Type) -> Boolean = { it is IntT }
  override val resCheck: (Type) -> Boolean = { it is CharT }
  override val argType: Type = IntT
  override val retType: Type = CharT
  override fun toString(): String = "chr"
}

// <binary-oper>
sealed class BinaryOper {
  internal abstract val validArgs: (Type, Type) -> Boolean
  internal abstract val validReturn: (Type) -> Boolean
  internal inline fun <reified T : Type> check(t1: Type, t2: Type) = t1 is T && t2 is T
  abstract val retType: Type
  abstract val inTypes: List<Type>
}

// (int, int) -> int:
sealed class IntBinOp : BinaryOper() {
  override val validArgs: (Type, Type) -> Boolean = { i1, i2 -> check<IntT>(i1, i2) }
  override val validReturn: (Type) -> Boolean = { it is IntT }
  override val inTypes = listOf(IntT)
  override val retType = IntT
}

// (int, int)/(char, char) -> int
sealed class CompBinOp : BinaryOper() {
  override val validArgs: (Type, Type) -> Boolean = { i1, i2 ->
    check<IntT>(i1, i2) || check<CharT>(i1, i2)
  }
  override val validReturn: (Type) -> Boolean = { it is BoolT }
  override val inTypes = listOf(IntT, CharT)
  override val retType = BoolT
}

// (bool, bool) -> bool:
sealed class BoolBinOp : BinaryOper() {
  override val validArgs: (Type, Type) -> Boolean = { b1, b2 -> check<BoolT>(b1, b2) }
  override val validReturn: (Type) -> Boolean = { it is IntT }
  override val inTypes = listOf(BoolT)
  override val retType = BoolT
}

object TimesBO : IntBinOp() {
  override fun toString(): String = "*"
}

object DivisionBO : IntBinOp() {
  override fun toString(): String = "/"
}

object ModBO : IntBinOp() {
  override fun toString(): String = "%"
}

object PlusBO : IntBinOp() {
  override fun toString(): String = "+"
}

object MinusBO : IntBinOp() {
  override fun toString(): String = "-"
}

// (int, int) -> bool:
object GtBO : CompBinOp() {
  override fun toString(): String = ">"
}

object GeqBO : CompBinOp() {
  override fun toString(): String = ">="
}

object LtBO : CompBinOp() {
  override fun toString(): String = "<"
}

object LeqBO : CompBinOp() {
  override fun toString(): String = ">="
}

sealed class EqualityBinOp : BinaryOper() {
  override val validArgs: (Type, Type) -> Boolean = { b1, b2 ->
    check<BoolT>(b1, b2) ||
      check<IntT>(b1, b2) ||
      check<CharT>(b1, b2) ||
      check<StringT>(b1, b2) ||
      check<AnyPairTs>(b1, b2) ||
      check<ArrayT>(b1, b2)
  }
  override val validReturn: (Type) -> Boolean = { it is BoolT }
  override val inTypes = listOf(
    IntT,
    BoolT,
    CharT,
    StringT,
    AnyArrayT(),
    AnyPairTs()
  )
  override val retType = BoolT
}

object EqBO : EqualityBinOp() {
  override fun toString(): String = "=="
}

object NeqBO : EqualityBinOp() {
  override fun toString(): String = "!="
}

object AndBO : BoolBinOp() {
  override fun toString(): String = "&&"
}

object OrBO : BoolBinOp() {
  override fun toString(): String = "||"
}
