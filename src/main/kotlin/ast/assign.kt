package ic.org.ast

import arrow.core.None
import arrow.core.firstOrNone
import ast.Sizes
import ic.org.arm.*
import ic.org.arm.addressing.ZeroOffsetAddrMode2
import ic.org.arm.addressing.withOffset
import ic.org.arm.addressing.zeroOffsetAddr
import ic.org.arm.instr.BLInstr
import ic.org.arm.instr.LDRInstr
import ic.org.arm.instr.MOVInstr
import ic.org.arm.instr.STRInstr
import ic.org.util.Code
import ic.org.util.NOT_REACHED
import ic.org.util.head
import ic.org.util.tail
import kotlinx.collections.immutable.toPersistentList

// <assign-lhs>
sealed class AssLHS {
  abstract val type: Type
  abstract val variable: Variable
}

data class IdentLHS(override val variable: Variable) : AssLHS() {
  override val type = variable.type
}

data class ArrayElemLHS(val indices: List<Expr>, override val variable: Variable) : AssLHS() {
  val ident = variable.ident
  override val type
    // Safe cast because caller validated that only arrays are accessed
    get() = (variable.type as ArrayT).nthNestedType(indices.size)
}

data class PairElemLHS(val pairElem: PairElem, override val variable: Variable, val pairs: PairT) : AssLHS() {
  override val type = when (pairElem) {
    is Fst -> pairs.fstT
    is Snd -> pairs.sndT
  }
}

interface Computable {
  val type: Type

  /**
   * Covert to [Code]. The result of evaluating this [Computable] should be put in [rem].
   * In order to perform the computation, one may use [rem], and dest's [Reg.next] registers.
   * If [Reg.next] happens to be [none], then the stack should be used.
   *
   * If this [Computable] is non basic (ie, a pair or an array) then the pointer to the structure is put in [rem].head
   */
  fun code(rem: Regs): Code

  /**
   * Evaluates an expression in order to put it in [dest]. Should be used by [Stat]
   */
  fun eval(dest: Register, usingRegs: List<Reg> = Reg.fromExpr) = Code.write {
    +code(usingRegs.toPersistentList())
    if (dest != usingRegs.head)
      +MOVInstr(rd = dest, op2 = usingRegs.head)
  }
}

sealed class AssRHS : Computable

data class ReadRHS(override val type: Type) : AssRHS() {
  override fun code(rem: Regs): Code = Code.write {
    val readFunc = when (type) {
      is IntT -> ReadInt
      is CharT -> ReadChar
      else -> NOT_REACHED()
    }

    +BLInstr(readFunc.label)
    +MOVInstr(rd = rem.head, op2 = Reg.ret)

    withFunction(readFunc)
  }
}

data class ExprRHS(val expr: Expr) : AssRHS(), Computable by expr {
  override fun toString() = expr.toString()
}

/**
 * If the array literal is empty, [arrT] is [EmptyArrayT]
 * Caller should verify content.
 */
data class ArrayLit(val exprs: List<Expr>, val arrT: AnyArrayT) : AssRHS() {
  override val type = arrT
  override fun code(rem: Regs) = Code.write {
    require(rem.size >= 2)

    val dst = rem.head
    // regs to be used by expressions
    val exprRem = rem.tail
    val exprType = exprs.firstOrNone().fold({ arrT }, { it.type })
    val exprSize = exprs.firstOrNone().fold({ 0 }, { it.type.size.bytes })
    val arrSize = exprs.size * exprSize

    // Load array size, add 4 bytes to store size
    +LDRInstr(Reg(0), arrSize + 4)
    // Malloc the array
    +BLInstr(MallocStdFunc.label)
    // Move the addr of the malloc (array) into the working reg
    +MOVInstr(None, false, dst, Reg(0))
    // For each expr, evaluate and put in array at index + exprSize
    exprs.forEachIndexed { i, expr ->
      +expr.code(exprRem)
      +exprType.sizedSTR(exprRem.head, dst.withOffset(4 + i * exprSize))
    }
    // Add array size to first slot
    +LDRInstr(exprRem.head, exprs.size)
    +STRInstr(exprRem.head, ZeroOffsetAddrMode2(dst))

    withFunction(MallocStdFunc)
  }

  override fun toString() =
    exprs.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.toString() }
}

data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS() {
  override val type = PairT(expr1.type, expr2.type)
  override fun toString() = "newpair($expr1, $expr2)"
  override fun code(rem: Regs) = Code.write {
    val dst = rem.head
    val rest = rem.tail
    +LDRInstr(Reg(0), 8)
    // Malloc and store addr in dst
    +BLInstr(MallocStdFunc.label)
    +MOVInstr(None, false, dst, Reg(0))
    // Compute the fst expr
    +expr1.code(rest)
    // Put result in pair fst slot
    +STRInstr(rest.head, dst.zeroOffsetAddr)
    // Compute the snd expr
    +expr2.code(rest)
    // Put result in pair snd slot
    +STRInstr(rest.head, dst.withOffset(Sizes.Word.bytes))
  }
}

data class PairElemRHS(val pairElem: PairElem, val pairs: PairT) : AssRHS() {
  override val type = when (pairElem) {
    is Fst -> pairs.fstT
    is Snd -> pairs.sndT
  }

  override fun code(rem: Regs) = Code.write {
    +pairElem.expr.code(rem)
    +MOVInstr(None, false, Reg(0), rem.head)
    +BLInstr(None, CheckNullPointer.label)
    +LDRInstr(rem.head, rem.head.withOffset(pairElem.offsetFromAddr))

    withFunction(CheckNullPointer)
  }

  override fun toString() = pairElem.toString()
}

data class Call(val func: FuncIdent, val args: List<Expr>) : AssRHS() {
  override val type = func.retType
  val name = func.name
  override fun code(rem: Regs) = Code.write {
    val (init, end, stackSize) = func.funcScope.makeInstrScope()
    args.zip(func.params).forEach { (argExpr, paramVar) ->
      // Offset corresponds to pram's address, minues 4b (because the stack grows when calling a function
      // minus the size of the function's stack (which will be compensated by when passing [init])
      val destAddr = SP.withOffset(paramVar.addrFromSP - stackSize - Sizes.Word.bytes)
      +argExpr.code(rem)
      +argExpr.type.sizedSTR(rem.head, destAddr)
    }
    +init
    +BLInstr(func.label)
    +end
    +MOVInstr(rd = rem.head, op2 = Reg.ret)
  }

  override fun toString() = "call ${func.name.name} (..)"
}
