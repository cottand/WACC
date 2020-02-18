package ic.org.ast

import ic.org.arm.*
import ic.org.util.Code
import org.antlr.v4.runtime.tree.TerminalNode
import kotlinx.collections.immutable.plus

// <program>
data class Prog(val funcs: List<Func>, val body: Stat)

// <func>
data class Func(val retType: Type, val ident: Ident, val params: List<Variable>, val stat: Stat) {

  val label = Label("f_" + ident.name)

  fun instr(): Code {
    val statCode = stat.instr()
    val body =
      label +
        PUSHInstr(LR) +
        statCode.instr +
        POPInstr(PC) +
        POPInstr(PC) +
        Directive.ltorg
    return Code(body, statCode.data)
  }
}

// <param>
data class Param(val type: Type, val ident: Ident)

// <pair-elem>
@Suppress("LeakingThis")
sealed class PairElem {
  abstract val expr: Expr

  val offsetFromAddr = when (this) {
    is Fst -> 0
    is Snd -> Type.Sizes.Word.bytes
  }

  companion object {
    fun fst(expr: Expr) = Fst(expr)
    fun snd(expr: Expr) = Snd(expr)
  }
}

data class Fst internal constructor(override val expr: Expr) : PairElem() {
  override fun toString() = "fst $expr"
}

data class Snd internal constructor(override val expr: Expr) : PairElem() {
  override fun toString() = "snd $expr"
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class Ident(val name: String) {
  constructor(node: TerminalNode) : this(node.text)

  override fun toString() = name
}
