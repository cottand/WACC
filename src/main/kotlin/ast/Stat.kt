package ic.org.ast

import ic.org.arm.*
import ic.org.util.Code
import ic.org.util.Position


sealed class Stat {
  abstract val scope: Scope
  abstract val pos: Position

  /**
   * Convert to an [Instr]
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
  override fun instr() = Code.empty +
    // Eval rhs in r0 for example
    // + ...
    // Save value in r4 in stack at address of variable
    // Look out for the size of STR...
  STRInstr(Reg(0), SP.withOffset(variable.addrFromSP))
}

data class Assign(val lhs: AssLHS, val rhs: AssRHS, override val scope: Scope, override val pos: Position) : Stat() {
  // See Decl.instr()
  override fun instr() = TODO()
}

data class Read(val lhs: AssLHS, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class Free(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class Return(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = Code.empty + expr.code(Reg.all)
}

data class Exit(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  // TODO revisit how do we determine which dest register [Expr] should use
  override fun instr() = expr.code(Reg.all) + BLInstr(Label("exit"))
}

data class Print(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class Println(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class If(val cond: Expr, val then: Stat, val `else`: Stat, override val scope: Scope, override val pos: Position) :
  Stat() {
  override fun instr() = TODO()
}

data class While(val cond: Expr, val stat: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class BegEnd(val stat: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class StatChain(val stat1: Stat, val stat2: Stat, override val scope: Scope, override val pos: Position) : Stat() {
  val thisStat = stat1
  val nextStat = stat2
  override fun instr() = thisStat.instr() + nextStat.instr()
}