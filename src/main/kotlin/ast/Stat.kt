package ic.org.ast

import ic.org.arm.Instr
import ic.org.util.Position
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.persistentListOf

// <stat>
sealed class Stat {
  abstract val scope: Scope
  abstract val pos: Position

  /**
   * Convert to an [Instr]
   */
  abstract fun instr(): PersistentList<Instr>
}

data class Skip(override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = persistentListOf<Nothing>()
}

data class Decl(val variable: Variable, val rhs: AssRHS, override val scope: Scope, override val pos: Position) :
  Stat() {
  val type = variable.type
  val ident = variable.ident
  override fun instr() = TODO()
}

data class Assign(val lhs: AssLHS, val rhs: AssRHS, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class Read(val lhs: AssLHS, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class Free(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class Return(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
}

data class Exit(val expr: Expr, override val scope: Scope, override val pos: Position) : Stat() {
  override fun instr() = TODO()
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