package ic.org.ast

import ic.org.arm.*
import ic.org.arm.instr.POPInstr
import ic.org.arm.instr.PUSHInstr
import ic.org.util.Code
import org.antlr.v4.runtime.tree.TerminalNode
import kotlinx.collections.immutable.plus

// <program>
data class Prog(val funcs: List<Func>, val body: Stat)

// <func>
data class Func(val retType: Type, val ident: Ident, val params: List<Variable>, val stat: Stat, val scope: Scope) {

  val label = Label("f_" + ident.name)

  fun instr(): Code {
    val statCode = stat.instr()
    val body =
      label +
        PUSHInstr(LR) +
        statCode.instr +
        POPInstr(PC) +
        Directive.ltorg
    return Code(body, statCode.data).withFunctions(statCode.funcs)
  }
}

// <param>
data class Param(val type: Type, val ident: Ident)

// <pair-elem>
sealed class PairElem {
  abstract val expr: Expr

  abstract val offsetFromAddr: Int

  companion object {
    fun fst(expr: Expr) = Fst(expr)
    fun snd(expr: Expr) = Snd(expr)
  }
}

data class Fst internal constructor(override val expr: Expr) : PairElem() {
  override fun toString() = "fst $expr"
  override val offsetFromAddr = 0
}

data class Snd internal constructor(override val expr: Expr) : PairElem() {
  override fun toString() = "snd $expr"
  override val offsetFromAddr = Type.Sizes.Word.bytes
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class Ident(val name: String) {
  constructor(node: TerminalNode) : this(node.text)

  override fun toString() = name
}
