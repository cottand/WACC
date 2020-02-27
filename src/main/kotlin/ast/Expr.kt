package ic.org.ast

import arrow.core.None
import arrow.core.extensions.list.foldable.forAll
import arrow.core.some
import arrow.core.valid
import ic.org.arm.*
import ic.org.arm.addressing.ImmEqualLabel
import ic.org.arm.addressing.ImmEquals32b
import ic.org.arm.addressing.zeroOffsetAddr
import ic.org.arm.instr.*
import ic.org.util.*
import java.lang.Integer.max
import java.lang.Integer.min

// <expr>
sealed class Expr : Computable {
  abstract val weight: Int

  companion object Weights {
    const val stackAccess = 2
    const val heapAccess = 2
  }
}

data class IntLit(val value: Int) : Expr() {
  override val type = IntT
  override fun code(rem: Regs) = Code.empty + LDRInstr(
    rem.head,
    ImmEquals32b(value)
  )

  override val weight = 1

  override fun toString(): String = value.toString()
}

data class BoolLit(val value: Boolean) : Expr() {
  override val type = BoolT
  override fun toString(): String = value.toString()
  // Booleans are represented as a 1 for true, and 0 for false
  override fun code(rem: Regs) = Code.write {
    +MOVInstr(rem.head, if (value) 1.toByte() else 0.toByte())
  }

  override val weight = 1
}

data class CharLit(val value: Char) : Expr() {
  override val type = CharT
  override fun toString(): String = "$value"
  // Chars are represented as their ASCII value
  override fun code(rem: Regs) = Code.write {
    +MOVInstr(rem.head, value)
  }

  override val weight = 1
}

data class StrLit(val value: String) : Expr() {
  override val type = StringT
  override fun toString(): String = value
  override fun code(rem: Regs): Code {
    val s = StringData(value, value.length - countEscape())
    val instr = LDRInstr(rem.head, ImmEqualLabel(s.label))
    return Code(data = s.body) + instr
  }

  override val weight = 1

  private fun countEscape(): Int {
    if (value.isEmpty())
      return 0
    var prev = value[0]
    var counter = 0
    for (c in value.drop(1)) {
      if (prev != '\\' && c == '\\')
        counter += 1
      prev = c
    }
    return counter
  }
}

object NullPairLit : Expr() {
  override val type = AnyPairTs()
  override fun toString(): String = "null"
  // null is represented as 0
  override fun code(rem: Regs) = Code.empty + LDRInstr(
    rem.head,
    ImmEquals32b(0)
  )

  override val weight = 1
}

data class IdentExpr(val vari: Variable, val scope: Scope) : Expr() {
  override val type = vari.type
  override fun toString(): String = vari.ident.name
  override fun code(rem: Regs): Code = Code.instr(vari.get(destReg = rem.head, currentScope = scope))
  override val weight = Weights.stackAccess
}

data class ArrayElemExpr internal constructor(
  val variable: Variable,
  val exprs: List<Expr>,
  val scope: Scope,
  override val type: Type
) : Expr() {
  override fun toString(): String = variable.ident.name +
    exprs.joinToString(separator = ", ", prefix = "[", postfix = "]")

  override fun code(rem: Regs): Code = rem.take2OrNone.fold({
    TODO()
  }, { (dst, nxt, _) ->
    val arrayAccessCode = Code.write {
      +MOVInstr(rd = Reg(0), op2 = nxt)  // Place the evaluated expr for index in r0
      +MOVInstr(rd = Reg(1), op2 = dst)  // Place the array variable in r1
      +BLInstr(CheckArrayBounds.label)
      // Increment dst (ident) so we skip the actual first element, the array size
      +ADDInstr(s = false, rd = dst, rn = dst, int8b = Type.Sizes.Word.bytes)
      +ADDInstr(None, false, dst, dst, LSLImmOperand2(nxt, Immed_5(log2(type.size.bytes))))
    }
    Code.write {
      +exprs.head.code(rem.tail)
      +variable.get(scope, dst)  // Place ident pointer in dst
      +arrayAccessCode
      exprs.tail.forEach { expr ->
        +expr.code(rem.tail)
        +LDRInstr(dst, dst.zeroOffsetAddr)
        +arrayAccessCode
      }
      +type.sizedLDR(dst, dst.zeroOffsetAddr)
    }
  }).withFunction(CheckArrayBounds.body)

  override val weight = Weights.heapAccess * exprs.size

  companion object {
    /**
     * Builds a type safe [ArrayElemExpr] from [variable]    */
    fun make(pos: Position, variable: Variable, exprs: List<Expr>, scope: Scope): Parsed<ArrayElemExpr> {
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
        else ->
          ArrayElemExpr(variable, exprs, scope, arrType.nthNestedType(exprs.size)).valid()
      }
    }
  }
}

data class UnaryOperExpr(val unaryOper: UnaryOper, val expr: Expr) : Expr() {
  override val type: Type = unaryOper.retType
  override val weight = expr.weight + 1

  override fun toString(): String = "$unaryOper $expr"
  override fun code(rem: Regs) = when (unaryOper) {
    NotUO -> Code.write {
      +expr.code(rem)
      +EORInstr(None, false, rem.head, rem.head, ImmOperand2(Immed_8r_bs(1, 0)))
    }
    MinusUO -> Code.write {
      +expr.code(rem)
      +RSBInstr(None, true, rem.head, rem.head, ImmOperand2(Immed_8r_bs(0, 0)))
      +BLInstr(VSCond.some(), OverflowException.label)
      withFunction(OverflowException)
    }
    LenUO -> Code.write {
      +expr.code(rem)
      +LDRInstr(rem.head, rem.head.zeroOffsetAddr)
    }
    OrdUO -> expr.code(rem)
    ChrUO -> expr.code(rem)
  }

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
  override val weight by lazy {
    val w1 = max(expr1.weight + 1, expr2.weight)
    val w2 = max(expr1.weight, expr2.weight + 1)
    min(w1, w2)
  }

  override fun toString(): String = "($expr1 $binaryOper $expr2)"
  override fun code(rem: Regs) = Code.write {
    rem.take2OrNone.fold({
      val dest = rem.head
      // No available registers, use stack machine! We can only use dest and Reg.last
      +expr2.code(rem)
      +PUSHInstr(dest)
      +ADDInstr(s = false, rd = SP, rn = SP, int8b = Type.Sizes.Word.bytes)
      +expr1.code(rem)
      +SUBInstr(s = false, rd = SP, rn = SP, int8b = Type.Sizes.Word.bytes)
      +POPInstr(Reg.last)
      +binaryOper.code(dest, Reg.last)
    }, { (dest, next, rest) ->
      // We can use at least two registers, dest and next, but we know there's more
      if (expr1.weight > expr2.weight) {
        +expr1.code(rem)
        +expr2.code(next prepend rest)
      } else {
        +expr2.code(next prepend (dest prepend rest))
        +expr1.code(dest prepend rest)
      }
      +binaryOper.code(dest, next)
    })
  }

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

  /**
   * The [Code] required in order to perform this [BinaryOper]. By convention, the result should be put in [dest]
   */
  abstract fun code(dest: Reg, r2: Reg): Code
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

object MulBO : IntBinOp() {
  override fun toString(): String = "*"
  override fun code(dest: Reg, r2: Reg) = Code.empty.withFunction(OverflowException.body) +
    SMULLInstr(None, false, dest, r2, dest, r2) +
    CMPInstr(None, r2, ASRImmOperand2(dest, Immed_5(31))) +
    BLInstr(NECond, OverflowException.label)
}

object DivBO : IntBinOp() {
  private val stdlibDiv = Label("__aeabi_idiv")
  override fun toString(): String = "/"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +MOVInstr(None, false, Reg(0), dest)
    +MOVInstr(None, false, Reg(1), r2)
    +BLInstr(None, CheckDivByZero.label)
    +BLInstr(None, stdlibDiv)
    +MOVInstr(None, false, dest, Reg(0))

    withFunction(CheckDivByZero)
  }
}

object ModBO : IntBinOp() {
  override fun toString(): String = "%"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    val stdlibMod = Label("__aeabi_idivmod")
    +MOVInstr(None, rd = Reg(0), op2 = Reg(4))
    +MOVInstr(None, rd = Reg(1), op2 = Reg(5))
    +BLInstr(None, CheckDivByZero.label)
    +BLInstr(None, stdlibMod)
    +MOVInstr(None, rd = Reg(4), op2 = Reg(1))

    withFunction(CheckDivByZero.body)
  }
}

object PlusBO : IntBinOp() {
  override fun toString(): String = "+"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +ADDInstr(rd = dest, rn = dest, op2 = r2)
    +BLInstr(VSCond, OverflowException.label)
    withFunction(OverflowException.body)
  }
}

object MinusBO : IntBinOp() {
  override fun toString(): String = "-"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +SUBInstr(rd = dest, rn = dest, op2 = r2)
    +BLInstr(VSCond, OverflowException.label)
    withFunction(OverflowException)
  }
}

// (int, int) -> bool:
object GtBO : CompBinOp() {
  override fun toString(): String = ">"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(GTCond, rd = dest, imm8b = 1)
    +MOVInstr(LECond, rd = dest, imm8b = 0)
  }
}

object GeqBO : CompBinOp() {
  override fun toString(): String = ">="
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(GECond, rd = dest, imm8b = 1)
    +MOVInstr(LTCond, rd = dest, imm8b = 0)
  }
}

object LtBO : CompBinOp() {
  override fun toString(): String = "<"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(LTCond, rd = dest, imm8b = 1)
    +MOVInstr(GECond, rd = dest, imm8b = 0)
  }
}

object LeqBO : CompBinOp() {
  override fun toString(): String = ">="
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(cond = LECond, rd = dest, imm8b = 1)
    +MOVInstr(cond = GTCond, rd = dest, imm8b = 0)
  }
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
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(cond = EQCond, rd = dest, imm8b = 1)
    +MOVInstr(cond = NECond, rd = dest, imm8b = 0)
  }
}

object NeqBO : EqualityBinOp() {
  override fun toString(): String = "!="
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(cond = NECond, rd = dest, imm8b = 1)
    +MOVInstr(cond = EQCond, rd = dest, imm8b = 0)
  }
}

object AndBO : BoolBinOp() {
  override fun toString(): String = "&&"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +ANDInstr(None, false, dest, dest, RegOperand2(r2))
  }
}

object OrBO : BoolBinOp() {
  override fun toString(): String = "||"
  override fun code(dest: Reg, r2: Reg) = Code.write {
    +ORRInstr(None, false, dest, dest, RegOperand2(r2))
  }
}
