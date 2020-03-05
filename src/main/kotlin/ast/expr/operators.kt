package ic.org.ast.expr

import arrow.core.None
import ic.org.arm.ASRImmOperand2
import ic.org.arm.CheckDivByZero
import ic.org.arm.EQCond
import ic.org.arm.GECond
import ic.org.arm.GTCond
import ic.org.arm.Immed_5
import ic.org.arm.LECond
import ic.org.arm.LTCond
import ic.org.arm.AsmLabel
import ic.org.arm.NECond
import ic.org.arm.OverflowException
import ic.org.arm.Reg
import ic.org.arm.RegOperand2
import ic.org.arm.VSCond
import ic.org.arm.instr.ADDInstr
import ic.org.arm.instr.ANDInstr
import ic.org.arm.instr.BLInstr
import ic.org.arm.instr.CMPInstr
import ic.org.arm.instr.MOVInstr
import ic.org.arm.instr.ORRInstr
import ic.org.arm.instr.SMULLInstr
import ic.org.arm.instr.SUBInstr
import ic.org.ast.AnyArrayT
import ic.org.ast.AnyPairTs
import ic.org.ast.ArrayT
import ic.org.ast.BoolT
import ic.org.ast.CharT
import ic.org.ast.IntT
import ic.org.ast.StringT
import ic.org.ast.Type
import ic.org.jvm.*
import ic.org.util.ARMAsm
import ic.org.util.shortRandomUUID

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

object NotUO : UnaryOper() {
  override val validArg: (Type) -> Boolean = { it is BoolT }
  override val resCheck: (Type) -> Boolean = { it is BoolT }
  override val argType: Type = BoolT
  override val retType: Type = BoolT
  override fun toString(): String = "!"
}

object MinusUO : UnaryOper() {
  override val validArg: (Type) -> Boolean = { it is IntT }
  override val resCheck: (Type) -> Boolean = { it is IntT }
  override val argType: Type = IntT
  override val retType: Type = IntT
  override fun toString(): String = "-"
}

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
   * The [ARMAsm] required in order to perform this [BinaryOper]. By convention, the result should be put in [dest]
   */
  abstract fun code(dest: Reg, r2: Reg): ARMAsm

  abstract fun jvmAsm() : JvmAsm
}

// (int, int) -> int:
sealed class IntBinOp : BinaryOper() {
  override val validArgs = { i1: Type, i2: Type -> check<IntT>(i1, i2) }
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
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    withFunction(OverflowException)
    +SMULLInstr(None, false, dest, r2, dest, r2)
    +CMPInstr(None, r2, ASRImmOperand2(dest, Immed_5(31)))
    +BLInstr(NECond, OverflowException.label)
  }

  override fun jvmAsm() = JvmAsm {
    +IMUL
  }
}

object DivBO : IntBinOp() {
  private val stdlibDiv = AsmLabel("__aeabi_idiv")
  override fun toString(): String = "/"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +MOVInstr(None, false, Reg(0), dest)
    +MOVInstr(None, false, Reg(1), r2)
    +BLInstr(None, CheckDivByZero.label)
    +BLInstr(None, stdlibDiv)
    +MOVInstr(None, false, dest, Reg(0))

    withFunction(CheckDivByZero)
  }

  override fun jvmAsm() = JvmAsm {
    +IDIV
  }
}

object ModBO : IntBinOp() {
  override fun toString(): String = "%"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    val stdlibMod = AsmLabel("__aeabi_idivmod")
    +MOVInstr(None, rd = Reg(0), op2 = Reg(4))
    +MOVInstr(None, rd = Reg(1), op2 = Reg(5))
    +BLInstr(None, CheckDivByZero.label)
    +BLInstr(None, stdlibMod)
    +MOVInstr(None, rd = Reg(4), op2 = Reg(1))

    withFunction(CheckDivByZero.body)
  }

  override fun jvmAsm() = JvmAsm {
    +IREM
  }
}

object PlusBO : IntBinOp() {
  override fun toString(): String = "+"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +ADDInstr(rd = dest, rn = dest, op2 = r2)
    +BLInstr(VSCond, OverflowException.label)
    withFunction(OverflowException.body)
  }

  override fun jvmAsm() = JvmAsm  {
    +IADD
  }
}

object MinusBO : IntBinOp() {
  override fun toString(): String = "-"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +SUBInstr(rd = dest, rn = dest, op2 = r2)
    +BLInstr(VSCond, OverflowException.label)
    withFunction(OverflowException)
  }

  override fun jvmAsm() = JvmAsm {
    +ISUB
  }
}

// (int, int) -> bool:
object GtBO : CompBinOp() {
  override fun toString(): String = ">"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(GTCond, rd = dest, imm8b = 1)
    +MOVInstr(LECond, rd = dest, imm8b = 0)
  }

  override fun jvmAsm() = TODO()
}

object GeqBO : CompBinOp() {
  override fun toString(): String = ">="
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(GECond, rd = dest, imm8b = 1)
    +MOVInstr(LTCond, rd = dest, imm8b = 0)
  }

  override fun jvmAsm() = JvmAsm {
    val labelTrue = JvmLabel("L_TRUE_" + shortRandomUUID())
    val labelSkip = JvmLabel("L_SKIP_" + shortRandomUUID())

    +IF_ICMPGE(labelTrue)
    +LDC.LDCInt(0)
    +GOTO(labelSkip)
    +labelTrue
    +LDC.LDCInt(1)
    +labelSkip
  }
}

object LtBO : CompBinOp() {
  override fun toString(): String = "<"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(LTCond, rd = dest, imm8b = 1)
    +MOVInstr(GECond, rd = dest, imm8b = 0)
  }

  override fun jvmAsm() = JvmAsm {
    val labelTrue = JvmLabel("L_TRUE_" + shortRandomUUID())
    val labelSkip = JvmLabel("L_SKIP_" + shortRandomUUID())

    +IF_ICMPLT(labelTrue)
    +LDC.LDCInt(0)
    +GOTO(labelSkip)
    +labelTrue
    +LDC.LDCInt(1)
    +labelSkip
  }
}

object LeqBO : CompBinOp() {
  override fun toString(): String = ">="
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +CMPInstr(None, dest, RegOperand2(r2))
    +MOVInstr(cond = LECond, rd = dest, imm8b = 1)
    +MOVInstr(cond = GTCond, rd = dest, imm8b = 0)
  }

  override fun jvmAsm() = JvmAsm {
    val labelTrue = JvmLabel("L_TRUE_" + shortRandomUUID())
    val labelSkip = JvmLabel("L_SKIP_" + shortRandomUUID())

    +IF_ICMPLE(labelTrue)
    +LDC.LDCInt(0)
    +GOTO(labelSkip)
    +labelTrue
    +LDC.LDCInt(1)
    +labelSkip
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
  override val inTypes = listOf(IntT, BoolT, CharT, StringT, AnyArrayT(), AnyPairTs())
  override val retType = BoolT
}

object EqBO : EqualityBinOp() {
  override fun toString(): String = "=="
  override fun code(dest: Reg, r2: Reg) =
    ARMAsm.write {
      +CMPInstr(None, dest, RegOperand2(r2))
      +MOVInstr(cond = EQCond, rd = dest, imm8b = 1)
      +MOVInstr(cond = NECond, rd = dest, imm8b = 0)
    }

  override fun jvmAsm() = JvmAsm {
    val labelTrue = JvmLabel("L_TRUE_" + shortRandomUUID())
    val labelSkip = JvmLabel("L_SKIP_" + shortRandomUUID())

    +IF_ICMPEQ(labelTrue)
    +LDC.LDCInt(0)
    +GOTO(labelSkip)
    +labelTrue
    +LDC.LDCInt(1)
    +labelSkip
  }
}

object NeqBO : EqualityBinOp() {
  override fun toString(): String = "!="
  override fun code(dest: Reg, r2: Reg) =
    ARMAsm.write {
      +CMPInstr(None, dest, RegOperand2(r2))
      +MOVInstr(cond = NECond, rd = dest, imm8b = 1)
      +MOVInstr(cond = EQCond, rd = dest, imm8b = 0)
    }

  override fun jvmAsm() = JvmAsm {
    val labelTrue = JvmLabel("L_TRUE_" + shortRandomUUID())
    val labelSkip = JvmLabel("L_SKIP_" + shortRandomUUID())

    +IF_ICMPNE(labelTrue)
    +LDC.LDCInt(0)
    +GOTO(labelSkip)
    +labelTrue
    +LDC.LDCInt(1)
    +labelSkip
  }
}

object AndBO : BoolBinOp() {
  override fun toString(): String = "&&"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +ANDInstr(None, false, dest, dest, RegOperand2(r2))
  }

  override fun jvmAsm() = JvmAsm {
    +IAND
  }
}

object OrBO : BoolBinOp() {
  override fun toString(): String = "||"
  override fun code(dest: Reg, r2: Reg) = ARMAsm.write {
    +ORRInstr(None, false, dest, dest, RegOperand2(r2))
  }

  override fun jvmAsm() = JvmAsm {
    +IOR
  }
}