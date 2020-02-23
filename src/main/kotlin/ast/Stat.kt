package ic.org.ast

import arrow.core.None
import arrow.core.some
import ic.org.arm.*
import ic.org.util.*

sealed class Stat {
  abstract val scope: Scope
  abstract val pos: Position

  /**
   * Convert to [Code]
   */
  abstract fun instr(): Code
}

data class Skip(override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = Code.empty
}

data class Decl(val variable: Variable, val rhs: AssRHS, override val scope: Scope, override val pos: Position) :
  Stat() {
  val type = variable.type
  val ident = variable.ident
  override fun instr() = variable.set(rhs, scope)
}

data class Assign(val lhs: AssLHS, val rhs: AssRHS, override val scope: Scope, override val pos: Position) : Stat() {
  // See Decl.instr()
  override fun instr() = Code.empty +
          when (lhs) {
            is IdentLHS -> lhs.variable.set(rhs, scope)
            is ArrayElemLHS -> TODO()
            is PairElemLHS -> Code.empty.withFunction(CheckNullPointer.body) +
                    rhs.eval(Reg(1)) + // Put RHS expr in r1
                    lhs.variable.get(scope, Reg(0)) + // Put Pair Ident in r0 (which is an addr)
                    BLInstr(None, CheckNullPointer.label) +
                    // STR r1 [r0 + pairElemOffset]
                    rhs.type.sizedSTR(Reg(1), Reg(0).withOffset(lhs.pairElem.offsetFromAddr))
          }
}

data class Read(val lhs: AssLHS, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() =
    when (lhs.type) {
      is IntT -> Code.empty.withFunction(ReadIntStdFunc.body) +
              ADDInstr(None, false, Reg(4), SP, 0) +
              MOVInstr(None, false, rd = Reg.ret, op2 = Reg(4)) +
              BLInstr(ReadIntStdFunc.label)
      is CharT -> Code.empty.withFunction(ReadCharStdFunc.body) +
              ADDInstr(None, false, Reg(4), SP, 0) +
              MOVInstr(None, false, rd = Reg.ret, op2 = Reg(4)) +
              BLInstr(ReadCharStdFunc.label)
      else -> TODO()
    }
}

data class Free(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = when (expr.type) {
    is AnyPairTs -> Code.empty.withFunction(FreePairFunc.body) +
            expr.code(Reg.fromExpr) +
            MOVInstr(rd = Reg.first, op2 = Reg.firstExpr) +
            BLInstr(FreePairFunc.label)
    else -> TODO()
  }
}

data class Return(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = expr.eval(Reg.ret) + POPInstr(PC)
}

data class Exit(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = expr.eval(Reg.ret) + BLInstr(Label("exit"))
}

data class Print(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr(): Code =
    when (expr.type) {
      is IntT -> expr.eval(Reg.ret).withFunction(PrintIntStdFunc.body) +
              BLInstr(PrintIntStdFunc.label)
      is BoolT -> expr.code(Reg.all).withFunction(PrintBoolStdFunc.body) +
              BLInstr(PrintBoolStdFunc.label)
      is CharT -> expr.code(Reg.all) +
              BLInstr(Label("putchar"))
      is StringT -> expr.code(Reg.all).withFunction(PrintStringStdFunc.body) +
              BLInstr(PrintStringStdFunc.label)
      is ArrayT -> if ((expr.type as ArrayT).type is CharT) {
        expr.code(Reg.all).withFunction(PrintStringStdFunc.body) +
                BLInstr(PrintStringStdFunc.label)
      } else {
        expr.code(Reg.all).withFunction(PrintReferenceStdFunc.body) +
                BLInstr(PrintReferenceStdFunc.label)
      }
      is AnyPairTs -> expr.code(Reg.all).withFunction(PrintReferenceStdFunc.body) +
              BLInstr(PrintReferenceStdFunc.label)
      else -> NOT_REACHED()
    }
}

data class Println(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = Print(expr, scope, pos).instr().withFunction(PrintLnStdFunc.body) +
          BLInstr(PrintLnStdFunc.label)
}

data class If(val cond: Expr, val then: Stat, val `else`: Stat, override val scope: Scope, override val pos: Position) :
  Stat() {
  override fun instr(): Code {
    val uuid = shortRandomUUID()
    val (line, _) = pos
    val jumpElse = Label("if_else_${uuid}_at_line_$line")
    val jumpContinue = Label("if_continue_${uuid}_at_line_$line")
    val (initThen, endThen) = then.scope.makeInstrScope()
    val (initElse, endElse) = `else`.scope.makeInstrScope()

    // This assumes cond.code will have made the actual comparison...
    return cond.code(Reg.fromExpr) +
            CMPInstr(cond = None, rn = Reg.firstExpr, int8b = 0) + // Test whether cond == 0
            BInstr(cond = EQCond.some(), label = jumpElse) + /// If so, jump to the else (cond was false!)
            initThen + // init the scope stack
            then.instr() +
            endThen + // end the scope stack
            BInstr(cond = None, label = jumpContinue) + // exit the if
            jumpElse + // begin `else`
            initElse + // same as the `then` branch
            `else`.instr() +
            endElse +
            jumpContinue
  }
}

data class While(val cond: Expr, val stat: Stat, override val pos: Position) : Stat() {
  override val scope = stat.scope
  override fun instr(): Code {
    val uuid = shortRandomUUID()
    val (line, _) = pos
    val (initScope, endScope) = scope.makeInstrScope()
    val bodyLabel = Label("while_body_${uuid}_at_line_$line")
    val condLabel = Label("while_cond_${uuid}_at_line_$line")

    return Code.empty +
            BInstr(cond = None, label = condLabel) +
            bodyLabel +
            initScope +
            stat.instr() +
            endScope +
            condLabel +
            cond.eval(Reg(4)) +
            CMPInstr(cond = None, rn = Reg(4), int8b = 1) + // Test whether cond == 1
            BInstr(cond = EQCond.some(), label = bodyLabel) // If yes, jump to body
  }
}

data class BegEnd(val stat: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = scope.makeInstrScope().let { (init, end) ->
    Code.empty + init + stat.instr() + end
  }
}

data class StatChain(val stat1: Stat, val stat2: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  val thisStat = stat1
  val nextStat = stat2
  override fun instr() = thisStat.instr() + nextStat.instr()
}