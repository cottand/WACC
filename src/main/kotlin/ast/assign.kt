package ic.org.ast

import ic.org.arm.*
import ic.org.util.Code
import ic.org.util.flatten
import ic.org.util.mapp

// <assign-lhs>
sealed class AssLHS {
  abstract val type: Type
}

data class IdentLHS(val variable: Variable) : AssLHS() {
  override val type = variable.type
}

data class ArrayElemLHS(val indices: List<Expr>, val variable: Variable) : AssLHS() {
  val ident = variable.ident
  override val type
    // Safe cast because caller validated that only arrays are accessed
    get() = (variable.type as ArrayT).nthNestedType(indices.size)
}

data class PairElemLHS(val pairElem: PairElem, val variable: Variable, val pairs: PairT) : AssLHS() {
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
   */
  fun code(rem: Regs): Code
}

sealed class AssRHS : Computable

data class ExprRHS(val expr: Expr) : AssRHS(), Computable by expr {
  override fun toString() = expr.toString()
}

/**
 * If the array literal is empty, [arrT] is [EmptyArrayT]
 * Caller should verify content.
 */
data class ArrayLit(val exprs: List<Expr>, val arrT: AnyArrayT) : AssRHS() {
  override val type = arrT
  override fun code(rem: Regs) = TODO()

  override fun toString() =
    exprs.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.toString() }
}

data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS() {
  override val type = PairT(expr1.type, expr2.type)
  override fun toString() = "newpair($expr1, $expr2)"
  override fun code(rem: Regs) = TODO()
}

data class PairElemRHS(val pairElem: PairElem, val pairs: PairT) : AssRHS() {
  override val type = when (pairElem) {
    is Fst -> pairs.fstT
    is Snd -> pairs.sndT
  }

  override fun code(rem: Regs) = TODO()
  override fun toString() = pairElem.toString()
}

data class Call(val func: FuncIdent, val args: List<Expr>) : AssRHS() {
  override val type = func.retType
  val name = func.name
  // TODO this stat.code() has to do BL instr AND also add 4 to the stack pointer. See reference assembly code...
  override fun code(rem: Regs) = func.funcScope.makeInstrScope().let { (init, end) ->
    Code.empty +
      init +
      args.mapIndexed { i, expr ->
        // val (instr, data) =
        val param = func.params[i]
        expr.code(Reg.all) +
          when (expr.type.size) { // TODO fix address that does not take into account the
            Type.Sizes.Word -> STRInstr(Reg.first, SP.withOffset(param.addrFromSP))
            Type.Sizes.Char -> TODO()
          }
      }.flatten() +
    BLInstr(func.label) +
    end
  }

  override fun toString() = "call ${func.name.name} (..)"
}
