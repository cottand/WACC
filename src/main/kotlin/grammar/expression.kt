package ic.org.grammar

import arrow.core.extensions.list.foldable.find
import arrow.core.extensions.list.foldable.forAll
import arrow.core.getOrElse
import arrow.core.valid
import ic.org.*
import java.lang.IllegalArgumentException

// <expr>
sealed class Expr {
  abstract val type: Type
}

data class IntLit(val value: Int) : Expr() {
  override val type = IntT

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
}

data class CharLit(val value: Char) : Expr() {
  override val type = CharT
}

data class StrLit(val value: String) : Expr() {
  override val type = StringT
}

object NullPairLit : Expr() {
  override val type = AnyPairTs() // TODO double check
}

data class IdentExpr(val vari: Variable) : Expr() {
  override val type = vari.type
}

data class ArrayElemExpr internal constructor(
    val variable: Variable,
    val exprs: List<Expr>,
    override val type: Type
) : Expr() {
  companion object {
    /**
     * Builds a type safe [ArrayElemExpr] from [variable]    */
    fun make(pos: Position, variable: Variable, exprs: List<Expr>): Parsed<ArrayElemExpr> {
      val arrType = variable.type
      return when {
        // Array access indexes must evaluate to ints
        !exprs.forAll { it.type == IntT } -> {
          val badExpr =
            exprs.find { it.type != IntT }.getOrElse { throw IllegalArgumentException() }
          IllegalArrayAccess(pos, badExpr.toString(), badExpr.type).toInvalidParsed()
        }
        arrType !is ArrayT -> TODO("Illegal type exception")
        exprs.size > arrType.depth -> TODO("Illegal type exception")
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

  override fun toString(): String = "$expr1 $binaryOper $expr2"

  companion object {
    /**
     * Factory method for safely building a [BinaryOperExpr] from a [BinaryOper]
     *
     * Makes sure [e1] and [e2] are the [Type]s that [binOp] expects and returns a [Parsed]
     * accordingly.
     */
    fun build(e1: Expr, binOp: BinaryOper, e2: Expr, pos: Position): Parsed<BinaryOperExpr> =
      if (binOp.validArgs(e1.type, e2.type))
        BinaryOperExpr(e1, binOp, e2).valid()
      else
        TypeError(pos, binOp.inTypes, e1.type to e2.type, binOp.toString()).toInvalidParsed()
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
}

// char -> int:
object OrdUO : UnaryOper() { // ord
  override val validArg: (Type) -> Boolean = { it is CharT }
  override val resCheck: (Type) -> Boolean = { it is IntT }
  override val argType: Type = CharT
  override val retType: Type = IntT
}

// int -> char:
object ChrUO : UnaryOper() { // chr
  override val validArg: (Type) -> Boolean = { it is IntT }
  override val resCheck: (Type) -> Boolean = { it is CharT }
  override val argType: Type = IntT
  override val retType: Type = CharT
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
    // TODO is (int, char) ok????
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

object TimesBO : IntBinOp() { // *
  override fun toString(): String = "*"
}

object DivisionBO : IntBinOp() // /
object ModBO : IntBinOp() // %
object PlusBO : IntBinOp() // +
object MinusBO : IntBinOp() {
  override fun toString(): String = "-"
} // -

// (int, int) -> bool:
object GtBO : CompBinOp() // >

object GeqBO : CompBinOp() // >=
object LtBO : CompBinOp() // <
object LeqBO : CompBinOp() // <=

sealed class EqualityBinOp : BinaryOper() {
  override val validArgs: (Type, Type) -> Boolean = { b1, b2 ->
    EqBO.check<BoolT>(b1, b2) ||
      EqBO.check<IntT>(b1, b2) ||
      EqBO.check<CharT>(b1, b2) ||
      EqBO.check<StringT>(b1, b2) ||
      EqBO.check<AnyPairTs>(b1, b2)
    // TODO? || check<ArrayT>(b1, b2)
  }
  override val validReturn: (Type) -> Boolean = { it is BoolT }
  override val inTypes = listOf(IntT, BoolT, CharT, StringT, AnyArrayT()) // TODO add?
  override val retType = BoolT
}

object EqBO : EqualityBinOp() // ==

object NeqBO : EqualityBinOp() // !=

object AndBO : BoolBinOp() // &&

object OrBO : BoolBinOp() // ||
