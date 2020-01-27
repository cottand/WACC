package ic.org.grammar

import arrow.core.extensions.list.foldable.find
import arrow.core.extensions.list.foldable.forAll
import arrow.core.getOrElse
import arrow.core.valid
import ic.org.*
import ic.org.grammar.ControlFlowScope.Variable
import java.lang.IllegalArgumentException

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
sealed class Stat() {
  abstract val scope: Scope
}

data class Skip(override val scope: Scope) : Stat()
data class Decl(val type: Type, val id: Ident, val rhs: AssRHS, override val scope: Scope) : Stat()
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
data class StatChain(val stat1: Stat, val stat2: Stat, override val scope: Scope) : Stat()

// <assign-lhs>
sealed class AssLHS

data class IdentLHS(val ident: Ident) : AssLHS()
data class ArrauElemLHS(val arrayElem: ArrayElem) : AssLHS()
data class PairElemLHS(val pairElem: PairElem) : AssLHS()

// <assign-rhs>
sealed class AssRHS

data class ExprRHS(val expr: Expr) : AssRHS()
data class ArrayLit(val exprs: List<Expr>) : AssRHS()
data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS()
data class PairElemRHS(val pairElem: PairElem) : AssRHS()
data class Call(val id: Ident, val args: List<Expr>) : AssRHS()

// <pair-elem>
sealed class PairElem

data class Fst(val expr: Expr) : PairElem()
data class Snd(val expr: Expr) : PairElem()

// <type>
sealed class Type

sealed class BaseT : Type()
open class AnyArrayT : Type() {
  //fun isAlsoArray(other: Type) = other is AnyArrayT
}

data class ArrayT(val type: Type, val depth: Int = 1) : AnyArrayT() {
  init {
    require(depth > 0)
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
      else -> throw IllegalArgumentException("Bad depth: $ndepth, not in $depth")
    }
  }
}

data class PairT(val fstT: PairElemT, val sndT: PairElemT) : Type()

// <base-type>
object IntT : BaseT()

object BoolT : BaseT()
object CharT : BaseT()
object StringT : BaseT()

// <pair-elem-type>
sealed class PairElemT

data class PairElemBaseT(val baseType: BaseT) : PairElemT()
data class PairElemArrayT(val arrayType: ArrayT) : PairElemT()
object NDPairT : PairElemT()

// <expr>
sealed class Expr {
  abstract val type: Type
}

data class IntLit(val value: Int) : Expr() {
  override val type = IntT
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
  override val type = TODO()
}

data class IdentExpr(val vari: Variable) : Expr() {
  override val type = vari.type
}

data class ArrayElemExpr internal constructor(
  val ident: Ident,
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
    fun make(pos: Position, ident: Ident, exprs: List<Expr>, scope: Scope): Parsed<ArrayElemExpr> {
      return scope[ident].fold({
        //ifEmpty
        VarNotFoundError(pos, ident.name).toInvalidParsed()
      }, { variable ->
        val arrType = variable.type
        when {
          !exprs.forAll { it.type == IntT } -> {
            val badExpr =
              exprs.find { it.type != IntT }.getOrElse { throw IllegalArgumentException() }
            IllegalArrayAccess(pos, badExpr.toString(), badExpr.type).toInvalidParsed()
          }
          arrType !is ArrayT -> TODO("Illegal type exception")
          exprs.size > arrType.depth -> TODO("Illegal type e")
          else -> ArrayElemExpr(ident, exprs, arrType.nthNestedType(exprs.size)).valid()
        }
      })
      // Array access indexes must evaluate to ints
    }
  }
}

data class UnaryOperExpr(val unaryOper: UnaryOper, val expr: Expr) : Expr() {
  override val type: Type = expr.type

  companion object {
    fun make(e: Expr, unOp: UnaryOper, pos: Position): Parsed<UnaryOperExpr> = TODO()
    //when {
    //  // Special case because the unary operator len accepts any array
    //  unOp.argType is AnyArrayT ->
    //    TypeError(pos, binOp.inTypes, e1.type, binOp.toString()).toInvalidParsed()
    //  else -> BinaryOperExpr(e2, binOp, e2).valid()
    //}
  }
}

data class BinaryOperExpr internal constructor(
  val expr1: Expr,
  val binaryOper: BinaryOper,
  val expr2: Expr
) : Expr() {
  override val type = binaryOper.retType

  companion object {
    fun make(e1: Expr, binOp: BinaryOper, e2: Expr, pos: Position): Parsed<BinaryOperExpr> =
      when {
        e1.type !in binOp.inTypes ->
          TypeError(pos, binOp.inTypes, e1.type, binOp.toString()).toInvalidParsed()
        e2.type !in binOp.inTypes ->
          TypeError(pos, binOp.inTypes, e2.type, binOp.toString()).toInvalidParsed()
        e1.type != e2.type ->
          UndefinedOp(pos, binOp.toString(), e1.type, e2.type).toInvalidParsed()
        else -> BinaryOperExpr(e2, binOp, e2).valid()
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
}

// int -> char:
object ChrUO : UnaryOper()       // chr
{
  override val argType: Type = IntT
  override val retType: Type = CharT
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
object MinusBO : IntBinOp()    // -

// (int, int) -> bool:
object GtBO : CompBinOp()       // >

object GeqBO : CompBinOp()      // >=
object LtBO : CompBinOp()       // <
object LeqBO : CompBinOp()      // <=
object EqBO : BinaryOper() {       // ==
  override val inTypes = listOf(IntT, BoolT, CharT, StringT, TODO("Add?"))
  override val retType = BoolT
}

object NeqBO : BinaryOper() {      // !=
  override val inTypes = listOf(IntT, BoolT, CharT, StringT, TODO("Add?"))
  override val retType = BoolT
}

// (bool, bool) -> bool:
object AndBO : BoolBinOp()      // &&

object OrBO : BoolBinOp()       // ||

// <ident>
data class Ident(val name: String)

// <array-elem>
data class ArrayElem(
  val ident: Ident,
  val indices: List<Expr>
)

