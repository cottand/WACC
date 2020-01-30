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
  // PRE: exprs.size >= 1
  //override val type: Type = when {
  //  exprs.size == 1 -> ident.type
  //  ident.type !is ArrayT ->
  //    throw IllegalArgumentException("Called several array getters on a non nested array")
  //  else -> ident.type.nthNestedType(exprs.size) // TODO check. It's scketchy
  //}

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
  override val type: Type = expr.type

  override fun toString(): String = "$unaryOper $expr"

  companion object {
    fun make(e: Expr, unOp: UnaryOper, pos: Position): Parsed<UnaryOperExpr> =
      when {
        e.type != unOp.argType ->
          TypeError(pos, unOp.argType, e.type, unOp.toString()).toInvalidParsed()
        else -> UnaryOperExpr(unOp, e).valid()
      }
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
    fun make(e1: Expr, binOp: BinaryOper, e2: Expr, pos: Position): Parsed<BinaryOperExpr> =
      when {
        e1.type !in binOp.inTypes ->
          TypeError(pos, binOp.inTypes, e1.type, binOp.toString()).toInvalidParsed()
        e2.type !in binOp.inTypes ->
          TypeError(pos, binOp.inTypes, e2.type, binOp.toString()).toInvalidParsed()
        e1.type != e2.type ->
          UndefinedOp(pos, binOp.toString(), e1.type, e2.type).toInvalidParsed()
        else -> BinaryOperExpr(e1, binOp, e2).valid()
      }
  }
}

// <unary-oper>
sealed class UnaryOper {
  abstract val argType: Type
  abstract val retType: Type
}

// bool:
object NotUO : UnaryOper()       // !
{
  override val argType: Type = BoolT
  override val retType: Type = BoolT
}

// int:
object MinusUO : UnaryOper()     // -
{
  override val argType: Type = IntT
  override val retType: Type = IntT
  override fun toString(): String = "-"
}

// arr -> int:
object LenUO : UnaryOper()       // len
{
  override val argType: Type = AnyArrayT()
  override val retType: Type = IntT
}

// char -> int:
object OrdUO : UnaryOper()       // ord
{
  override val argType: Type = CharT
  override val retType: Type = IntT
}

// int -> char:
object ChrUO : UnaryOper()       // chr
{
  override val argType: Type = IntT
  override val retType: Type = CharT
}

// <binary-oper>
sealed class BinaryOper {
  //abstract val argsTypes: Type
  abstract val retType: Type
  abstract val inTypes: List<Type>
}

sealed class IntBinOp : BinaryOper() {
  override val inTypes = listOf(IntT)
  override val retType = IntT
}

sealed class CompBinOp : BinaryOper() {
  override val inTypes = listOf(IntT, CharT)
  override val retType = BoolT
}

sealed class BoolBinOp : BinaryOper() {
  override val inTypes = listOf(BoolT)
  override val retType = BoolT
}

// (int, int) -> int:
object TimesBO : IntBinOp()    // *

object DivisionBO : IntBinOp() // /
object ModBO : IntBinOp()      // %
object PlusBO : IntBinOp()     // +
object MinusBO : IntBinOp() {
  override fun toString(): String = "-"
}    // -

// (int, int) -> bool:
object GtBO : CompBinOp()       // >

object GeqBO : CompBinOp()      // >=
object LtBO : CompBinOp()       // <
object LeqBO : CompBinOp()      // <=
object EqBO : BinaryOper() {       // ==
  override val inTypes = listOf(IntT, BoolT, CharT, StringT) //TODO add?
  override val retType = BoolT
}

object NeqBO : BinaryOper() {      // !=
  override val inTypes = listOf(IntT, BoolT, CharT, StringT) // TODO add?
  override val retType = BoolT
}

// (bool, bool) -> bool:
object AndBO : BoolBinOp()      // &&

object OrBO : BoolBinOp()       // ||
