package ic.org.ast.expr

import arrow.core.None
import arrow.core.extensions.list.foldable.forAll
import arrow.core.some
import arrow.core.valid
import ast.Sizes
import ic.org.arm.CheckArrayBounds
import ic.org.arm.ImmOperand2
import ic.org.arm.Immed_5
import ic.org.arm.Immed_8r_bs
import ic.org.arm.LSLImmOperand2
import ic.org.arm.OverflowException
import ic.org.arm.Reg
import ic.org.arm.Regs
import ic.org.arm.SP
import ic.org.arm.StringData
import ic.org.arm.VSCond
import ic.org.arm.addressing.ImmEqualLabel
import ic.org.arm.addressing.ImmEquals32b
import ic.org.arm.addressing.zeroOffsetAddr
import ic.org.arm.instr.ADDInstr
import ic.org.arm.instr.BLInstr
import ic.org.arm.instr.EORInstr
import ic.org.arm.instr.LDRInstr
import ic.org.arm.instr.MOVInstr
import ic.org.arm.instr.POPInstr
import ic.org.arm.instr.PUSHInstr
import ic.org.arm.instr.RSBInstr
import ic.org.arm.instr.SUBInstr
import ic.org.ast.AnyArrayT
import ic.org.ast.AnyPairTs
import ic.org.ast.ArrayT
import ic.org.ast.BoolT
import ic.org.ast.CharT
import ic.org.ast.Computable
import ic.org.ast.IntT
import ic.org.ast.Scope
import ic.org.ast.StringT
import ic.org.ast.Type
import ic.org.ast.Variable
import ic.org.jvm.*
import ic.org.util.ARMAsm
import ic.org.util.IllegalArrayAccess
import ic.org.util.NOT_REACHED
import ic.org.util.Parsed
import ic.org.util.Position
import ic.org.util.TypeError
import ic.org.util.head
import ic.org.util.log2
import ic.org.util.prepend
import ic.org.util.tail
import ic.org.util.take2OrNone
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
  override fun armAsm(rem: Regs) = ARMAsm.instr(LDRInstr(rem.head, ImmEquals32b(value)))

  override fun jvmAsm() = JvmAsm { +LDC(value) }

  override val weight = 1

  override fun toString(): String = value.toString()
}

data class BoolLit(val value: Boolean) : Expr() {
  override val type = BoolT
  override fun toString(): String = value.toString()
  // Booleans are represented as a 1 for true, and 0 for false
  override fun armAsm(rem: Regs) = ARMAsm.write {
    +MOVInstr(rem.head, if (value) 1.toByte() else 0.toByte())
  }

  override fun jvmAsm() = JvmAsm { +LDC(if (value) 1 else 0)}

  override val weight = 1
}

data class CharLit(val value: Char) : Expr() {
  override val type = CharT
  override fun toString(): String = "$value"
  // Chars are represented as their ASCII value
  override fun armAsm(rem: Regs) = ARMAsm.write {
    +MOVInstr(rem.head, value)
  }

  override fun jvmAsm() = JvmAsm {
    +BIPUSH(value.toByte())
  }

  override val weight = 1
}

data class StrLit(val value: String) : Expr() {
  override val type = StringT
  override fun toString(): String = value
  override fun armAsm(rem: Regs): ARMAsm {
    val s = StringData(value, value.length - countEscape())
    val instr = LDRInstr(rem.head, ImmEqualLabel(s.label))
    return ARMAsm(data = s.body) + instr
  }

  override fun jvmAsm() = JvmAsm { +LDC(value) }

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
  override fun armAsm(rem: Regs) = ARMAsm.instr(LDRInstr(rem.head, ImmEquals32b(0)))

  override fun jvmAsm() = TODO()

  override val weight = 1
}

data class IdentExpr(val vari: Variable, val scope: Scope) : Expr() {
  override val type = vari.type
  override fun toString(): String = vari.ident.name
  override fun armAsm(rem: Regs): ARMAsm = ARMAsm.instr(vari.get(destReg = rem.head, currentScope = scope))
  override fun jvmAsm() = vari.load()  // TODO is it just load?
  override val weight = stackAccess
}

data class ArrayElemExpr internal constructor(
  val variable: Variable,
  val exprs: List<Expr>,
  val scope: Scope,
  override val type: Type
) : Expr() {
  override fun toString(): String = variable.ident.name +
    exprs.joinToString(separator = ", ", prefix = "[", postfix = "]")

  override fun armAsm(rem: Regs): ARMAsm = rem.take2OrNone.fold({
    TODO()
  }, { (dst, nxt, _) ->
    val arrayAccessCode = ARMAsm.write {
      +MOVInstr(rd = Reg(0), op2 = nxt) // Place the evaluated expr for index in r0
      +MOVInstr(rd = Reg(1), op2 = dst) // Place the array variable in r1
      +BLInstr(CheckArrayBounds.label)
      // Increment dst (ident) so we skip the actual first element, the array size
      +ADDInstr(s = false, rd = dst, rn = dst, int8b = Sizes.Word.bytes)
      +ADDInstr(None, false, dst, dst, LSLImmOperand2(nxt, Immed_5(log2(type.size.bytes))))
    }
    ARMAsm.write {
      +exprs.head.armAsm(rem.tail)
      +variable.get(scope, dst) // Place ident pointer in dst
      +arrayAccessCode
      exprs.tail.forEach { expr ->
        +expr.armAsm(rem.tail)
        +LDRInstr(dst, dst.zeroOffsetAddr)
        +arrayAccessCode
      }
      +type.sizedLDR(dst, dst.zeroOffsetAddr)
    }
  }).withFunction(CheckArrayBounds.body)

  override fun jvmAsm() = TODO()

  override val weight = heapAccess * exprs.size

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
  override fun armAsm(rem: Regs) = when (unaryOper) {
    NotUO -> ARMAsm.write {
      +expr.armAsm(rem)
      +EORInstr(None, false, rem.head, rem.head, ImmOperand2(Immed_8r_bs(1, 0)))
    }
    MinusUO -> ARMAsm.write {
      +expr.armAsm(rem)
      +RSBInstr(None, true, rem.head, rem.head, ImmOperand2(Immed_8r_bs(0, 0)))
      +BLInstr(VSCond.some(), OverflowException.label)
      withFunction(OverflowException)
    }
    LenUO -> ARMAsm.write {
      +expr.armAsm(rem)
      +LDRInstr(rem.head, rem.head.zeroOffsetAddr)
    }
    OrdUO -> expr.armAsm(rem)
    ChrUO -> expr.armAsm(rem)
  }

  override fun jvmAsm() = TODO()

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
  override fun armAsm(rem: Regs) = ARMAsm.write {
    rem.take2OrNone.fold({
      val dest = rem.head
      // No available registers, use stack machine! We can only use dest and Reg.last
      +expr2.armAsm(rem)
      +PUSHInstr(dest)
      +ADDInstr(s = false, rd = SP, rn = SP, int8b = Sizes.Word.bytes)
      +expr1.armAsm(rem)
      +SUBInstr(s = false, rd = SP, rn = SP, int8b = Sizes.Word.bytes)
      +POPInstr(Reg.last)
      +binaryOper.code(dest, Reg.last)
    }, { (dest, next, rest) ->
      // We can use at least two registers, dest and next, but we know there's more
      if (expr1.weight > expr2.weight) {
        +expr1.armAsm(rem)
        +expr2.armAsm(next prepend rest)
      } else {
        +expr2.armAsm(next prepend (dest prepend rest))
        +expr1.armAsm(dest prepend rest)
      }
      +binaryOper.code(dest, next)
    })
  }

  override fun jvmAsm() = JvmAsm {
    +expr1.jvmAsm()
    +expr2.jvmAsm()
    +binaryOper.jvmAsm()
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

