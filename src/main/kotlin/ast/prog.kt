package ic.org.ast

import ast.Sizes
import ic.org.arm.AsmDirective
import ic.org.arm.LR
import ic.org.arm.AsmLabel
import ic.org.arm.PC
import ic.org.arm.Reg
import ic.org.arm.instr.LDRInstr
import ic.org.arm.instr.POPInstr
import ic.org.arm.instr.PUSHInstr
import ic.org.ast.expr.Expr
import ic.org.util.Code
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import org.antlr.v4.runtime.tree.TerminalNode

// <program>
data class Prog(val funcs: List<Func>, val body: Stat, val globalScope: GlobalScope) {
  fun asm() = body.instr()
    .withFunctions(funcs.map { it.instr() })
    .let {
      val allFuncs = it.funcs.fold(Code.empty, Code::combine)
      val allData = it.data + allFuncs.data
      val dataSegment = if (allData.isNotEmpty()) AsmDirective.data + allData else persistentListOf<Nothing>()
      val (initScope, endScope) = globalScope.makeInstrScope()
      dataSegment +
        AsmDirective.text +
        AsmDirective.main +
        AsmLabel("main") +
        PUSHInstr(LR) +
        initScope +
        it.instr +
        endScope +
        LDRInstr(Reg(0), 0) +
        POPInstr(PC) +
        AsmDirective.ltorg +
        // Function code segments:
        allFuncs.instr
    }.joinToString(separator = "\n", postfix = "\n") {
      val margin = when (it) {
        is AsmDirective, is AsmLabel -> "  "
        else -> "    "
      }
      margin + it.code
    }
}

// <func>
data class Func(val retType: Type, val ident: Ident, val params: List<Variable>, val stat: Stat, val scope: Scope) {

  val label = AsmLabel("f_" + ident.name)

  fun instr() = Code.write {
    val statCode = stat.instr()
    data { +statCode.data }
    +label
    +PUSHInstr(LR)
    +statCode.instr
    +POPInstr(PC)
    +AsmDirective.ltorg

    withFunctions(statCode.funcs)
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
  override val offsetFromAddr = Sizes.Word.bytes
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class Ident(val name: String) {
  constructor(node: TerminalNode) : this(node.text)

  override fun toString() = name
}
