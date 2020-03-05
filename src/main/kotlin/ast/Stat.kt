package ic.org.ast

import arrow.core.None
import arrow.core.some
import ast.Sizes
import ic.org.arm.*
import ic.org.arm.addressing.withOffset
import ic.org.arm.instr.*
import ic.org.ast.expr.Expr
import ic.org.jvm.*
import ic.org.util.*

sealed class Stat {
  abstract val scope: Scope
  abstract val pos: Position

  /**
   * Convert to [ARMAsm]
   */
  abstract fun instr(): ARMAsm

  /**
   * Convert to [JvmAsm]
   */
  abstract fun jvmInstr(): JvmAsm
}

data class Skip(override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = ARMAsm.empty
  override fun jvmInstr() = JvmAsm.empty
}

data class Decl(val variable: Variable, val rhs: AssRHS, override val scope: Scope, override val pos: Position) :
  Stat() {
  val type = variable.type
  val ident = variable.ident
  override fun instr() = variable.set(rhs, scope)
  override fun jvmInstr() = variable.store(rhs)
}

data class Assign(val lhs: AssLHS, val rhs: AssRHS, override val scope: Scope, override val pos: Position) : Stat() {
  // See Decl.instr()
  override fun instr() = when (lhs) {
    is IdentLHS -> lhs.variable.set(rhs, scope)
    is ArrayElemLHS -> ARMAsm.write {
      +lhs.variable.get(scope, Reg(4)) // Put array var in r4
      // Iteratively load array addresses into r0 for nested arrays
      lhs.indices.dropLast(1).forEach {
        +it.armAsm(Reg.fromExpr.tail) // Load index into r5
        +MOVInstr(rd = Reg.sndArg, op2 = Reg(4))
        +MOVInstr(None, false, Reg.fstArg, Reg(5))
        +BLInstr(CheckArrayBounds.label)
        +ADDInstr(None, false, Reg(4), Reg(4), Sizes.Word.bytes) // Add 4 bytes offset since 1st slot is array size
        +rhs.type.sizedLDR(Reg(4), Reg(4).withOffset(Reg(5), log2(rhs.type.size.bytes)))
      }
      // Get index of array elem by evaluating the last expression
      +lhs.indices.last().armAsm(Reg.fromExpr.tail) // Load index into r5
      +MOVInstr(None, false, Reg.sndArg, Reg(4))
      +MOVInstr(None, false, Reg.fstArg, Reg(5))
      +BLInstr(CheckArrayBounds.label)

      +ADDInstr(None, false, Reg(4), Reg(4), 4) // Add 4 bytes offset since 1st slot is array size
      +rhs.eval(Reg(6), Reg.fromExpr.drop(2)) // eval rhs into r6
      +rhs.type.sizedSTR(Reg(6), Reg(4).withOffset(Reg(5), log2(rhs.type.size.bytes)))
      withFunction(CheckArrayBounds)
    }
    is PairElemLHS -> ARMAsm.write {
      +rhs.eval(Reg(1)) // Put RHS expr in r1
      +lhs.variable.get(scope, Reg(0)) // Put Pair Ident in r0 (which is an addr)
      +BLInstr(None, CheckNullPointer.label)
      // STR r1 [r0 + pairElemOffset]
      +rhs.type.sizedSTR(Reg(1), Reg(0).withOffset(lhs.pairElem.offsetFromAddr))
      withFunction(CheckNullPointer)
    }
  }

  override fun jvmInstr() = when (lhs) {
    is IdentLHS -> JvmAsm { +lhs.variable.store(rhs) }
    is ArrayElemLHS -> TODO()
    is PairElemLHS -> TODO()
  }
}

data class Read(val lhs: AssLHS, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr(): ARMAsm = Assign(lhs, ReadRHS(lhs.type), scope, pos).instr()
  override fun jvmInstr() = TODO()
}

data class Free(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = when (expr.type) {
    is AnyPairTs -> ARMAsm.write {
      +expr.armAsm(Reg.fromExpr)
      +MOVInstr(rd = Reg.first, op2 = Reg.firstExpr)
      +BLInstr(FreeFunc.label)

      withFunction(FreeFunc)
    }
    is AnyArrayT -> ARMAsm.write {
      +expr.armAsm(Reg.fromExpr)
      +MOVInstr(rd = Reg.first, op2 = Reg.firstExpr)
      +BLInstr(FreeFunc.label)

      withFunction(FreeFunc)
    }

    else -> NOT_REACHED()
  }

  override fun jvmInstr() = TODO()
}

data class Return(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = expr.eval(Reg.ret) + POPInstr(PC)
  override fun jvmInstr() = expr.jvmAsm() + expr.type.toJvm().jvmReturn
}

data class Exit(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = expr.eval(Reg.ret) + BLInstr(AsmLabel("exit"))
  override fun jvmInstr() = expr.jvmAsm() + JvmSystemExit.invoke
}

data class Print(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = ARMAsm.write {
    val type = expr.type
    +expr.eval(Reg.first)
    when (type) {
      is IntT -> {
        withFunction(PrintIntStdFunc.body)
        +BLInstr(PrintIntStdFunc.label)
      }
      is BoolT -> {
        withFunction(PrintBoolStdFunc.body)
        +BLInstr(PrintBoolStdFunc.label)
      }
      is CharT -> +BLInstr(AsmLabel("putchar"))
      is StringT -> {
        withFunction(PrintStringStdFunc.body)
        +BLInstr(PrintStringStdFunc.label)
      }
      is AnyPairTs -> {
        withFunction(PrintReferenceStdFunc.body)
        +BLInstr(PrintReferenceStdFunc.label)
      }
      is ArrayT -> if (type.type is CharT) {
        withFunction(PrintStringStdFunc.body)
        +BLInstr(PrintStringStdFunc.label)
      } else {
        withFunction(PrintReferenceStdFunc.body)
        +BLInstr(PrintReferenceStdFunc.label)
      }
      else -> NOT_REACHED()
    }
  }

  override fun jvmInstr() = JvmAsm.write {
    val type = expr.type
    +expr.jvmAsm()
    when (type) {
      is IntT -> +JvmSystemPrintFunc(JvmInt).invoke
      is BoolT -> +JvmSystemPrintFunc(JvmBool).invoke
      is CharT -> +JvmSystemPrintFunc(JvmChar).invoke
      is StringT -> +JvmSystemPrintFunc(JvmString).invoke
      is AnyPairTs -> TODO()
      is ArrayT -> if (type.type is CharT) +JvmSystemPrintFunc(JvmString).invoke else TODO()
      is AnyArrayT -> TODO()
    }.let{}
  }
}

data class Println(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = ARMAsm.write {
    +Print(expr, scope, pos).instr()
    +BLInstr(PrintLnStdFunc.label)
    withFunction(PrintLnStdFunc)
  }

  override fun jvmInstr() = JvmAsm.write {
    val type = expr.type
    +expr.jvmAsm()
    when (type) {
      is IntT -> +JvmSystemPrintlnFunc(JvmInt).invoke
      is BoolT -> +JvmSystemPrintlnFunc(JvmBool).invoke
      is CharT -> +JvmSystemPrintlnFunc(JvmChar).invoke
      is StringT -> +JvmSystemPrintlnFunc(JvmString).invoke
      is AnyPairTs -> TODO()
      is ArrayT ->
        if (type.type is CharT) +JvmSystemPrintlnFunc(JvmString).invoke
        else TODO()
      // Empty array
      is AnyArrayT -> TODO()
    }

  }
}

data class If(val cond: Expr, val then: Stat, val `else`: Stat, override val scope: Scope, override val pos: Position) :
  Stat() {
  override fun instr() = ARMAsm.write {
    val uuid = shortRandomUUID()
    val (line, _) = pos
    val elseLabel = AsmLabel("if_else_${uuid}_at_line_$line")
    val continueLabel = AsmLabel("if_continue_${uuid}_at_line_$line")
    val (initThen, endThen) = then.scope.makeInstrScope()
    val (initElse, endElse) = `else`.scope.makeInstrScope()

    +cond.armAsm(Reg.fromExpr)
    +CMPInstr(cond = None, rn = Reg.firstExpr, int8b = 0) // Test whether cond == 0
    +BInstr(cond = EQCond.some(), label = elseLabel) // / If so, jump to the else (cond was false!)
    +initThen // init the scope stack
    +then.instr()
    +endThen // end the scope stack
    +BInstr(label = continueLabel) // exit the if
    +elseLabel // begin `else`
    +initElse // same as the `then` branch
    +`else`.instr()
    +endElse
    +continueLabel
  }

  override fun jvmInstr() = JvmAsm {
    val uuid = shortRandomUUID()
    val elseLabel = JvmLabel("IfElse_$uuid")
    val continueLabel = JvmLabel("IfContinue_$uuid")

    +cond.jvmAsm() // Load cond (bool) to top of the stack
    +IFEQ(elseLabel) // If cond == 0 (cond == false) jump to else
    +then.jvmInstr() // `then` body
    +GOTO(continueLabel) // exit `if`
    +elseLabel
    +`else`.jvmInstr() // `else` body
    +continueLabel
    +LDC(0) // dummy body so the continuation has something to jump to
    +POP
  }
}

data class While(val cond: Expr, val body: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = ARMAsm.write {
    val uuid = shortRandomUUID()
    val (line, _) = pos
    val (initScope, endScope) = body.scope.makeInstrScope()
    val bodyLabel = AsmLabel("while_body_${uuid}_at_line_$line")
    val condLabel = AsmLabel("while_cond_${uuid}_at_line_$line")

    +BInstr(cond = None, label = condLabel)
    +bodyLabel
    +initScope
    +body.instr()
    +endScope
    +condLabel
    +cond.eval(Reg(4))
    +CMPInstr(cond = None, rn = Reg(4), int8b = 1) // Test whether cond == 1
    +BInstr(cond = EQCond.some(), label = bodyLabel) // If yes, jump to body
  }

  override fun jvmInstr() = jvmAsmWithPos {
    val uuid = shortRandomUUID()
    val bodyLabel = JvmLabel("WhileBody_$uuid")
    val condLabel = JvmLabel("WhileCond_$uuid")

    +GOTO(condLabel)
    +bodyLabel
    +body.jvmInstr()
    +condLabel
    +cond.jvmAsm()
    +IFNE(bodyLabel) // If cond != 0 (cond is true) jump to body
  }
}

data class BegEnd(val body: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = ARMAsm.write {
    val (init, end) = body.scope.makeInstrScope()
    +init
    +body.instr()
    +end
  }

  override fun jvmInstr() = TODO()
}

data class StatChain(val stat1: Stat, val stat2: Stat, override val scope: Scope, override val pos: Position) :
  Stat() {
  val thisStat = stat1
  val nextStat = stat2
  override fun instr() = thisStat.instr() + nextStat.instr()
  override fun jvmInstr() = thisStat.jvmInstr() + nextStat.jvmInstr()
}
