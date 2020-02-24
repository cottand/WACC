package ic.org.ast

import arrow.core.None
import arrow.core.some
import ic.org.arm.*
import ic.org.ast.NullPairLit.type
import ic.org.util.*
import kotlinx.collections.immutable.toPersistentList

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
            is ArrayElemLHS -> Code.empty.withFunction(CheckArrayBounds.body) +
                    lhs.variable.get(scope, Reg(4)) + // Put array var in r4
                    // Iteratively load array addresses into r0 for nested arrays
                    lhs.indices.dropLast(1).map {
                      it.code(Reg.fromExpr.tail) + // Load index into r5

                      MOVInstr(None, false, Reg(1), Reg(4)) +
                      MOVInstr(None, false, Reg(0), Reg(5)) +
                      BLInstr(CheckArrayBounds.label) +
                      ADDInstr(None, false, Reg(4), Reg(4), 4) + // Add 4 bytes offset since 1st slot is array size
                      type.sizedLDR(Reg(4), Reg(4).withOffset(Reg(5), log2(type.size.bytes)))
                    }.flatten() +

                    // Get index of array elem by evaluating the last expression
                    lhs.indices.last().code(Reg.fromExpr.tail) + // Load index into r5
                    MOVInstr(None, false, Reg(1), Reg(4)) +
                    MOVInstr(None, false, Reg(0), Reg(5)) +
                    BLInstr(CheckArrayBounds.label) +

                    ADDInstr(None, false, Reg(4), Reg(4), 4) + // Add 4 bytes offset since 1st slot is array size
                    rhs.eval(Reg(6), Reg.fromExpr.drop(2)) + // eval rhs into r1
                    STRInstr(Reg(6), Reg(4).withOffset(Reg(5), log2(type.size.bytes))) // Put rhs into addr pointed by r0
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
    when(lhs.type) {
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
  override fun instr() =
    when (expr.type) {
      is IntT -> expr.eval(Reg.ret).withFunction(PrintIntStdFunc.body) +
        BLInstr(PrintIntStdFunc.label)
      is BoolT -> expr.code(Reg.all).withFunction(PrintBoolStdFunc.body) +
              BLInstr(PrintBoolStdFunc.label)
      is CharT -> expr.code(Reg.all) +
                BLInstr(Label("putchar"))
      is StringT -> expr.code(Reg.all).withFunction(PrintStringStdFunc.body) +
          BLInstr(PrintStringStdFunc.label)
      else -> TODO()
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