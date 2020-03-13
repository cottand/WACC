package ic.org.ast

import ast.Sizes
import ic.org.arm.AsmDirective
import ic.org.arm.AsmLabel
import ic.org.arm.LR
import ic.org.arm.PC
import ic.org.arm.Reg
import ic.org.arm.instr.LDRInstr
import ic.org.arm.instr.POPInstr
import ic.org.arm.instr.PUSHInstr
import ic.org.ast.expr.Expr
import ic.org.jvm.DefinedMethod
import ic.org.jvm.JvmAsm
import ic.org.jvm.JvmDirective
import ic.org.jvm.JvmGenOnly
import ic.org.jvm.JvmLabel
import ic.org.jvm.JvmStringInstr
import ic.org.jvm.Main
import ic.org.jvm.SuperObject
import ic.org.jvm.toJvm
import ic.org.util.ARMAsm
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import org.antlr.v4.runtime.tree.TerminalNode

// <program>
data class Prog(val name: String, val funcs: List<Func>, val body: Stat, val globalScope: GlobalScope) {
  fun armAsm() = body.instr()
    .withFunctions(funcs.map(Func::armAsm))
    .let {
      val allFuncs = it.funcs.fold(ARMAsm.empty, ARMAsm::combine)
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

  fun jvmAsm() = JvmAsm {
    +".source $name.wacc"
    +".class public $name"
    +SuperObject
    // +MainClass.init
    withMethods(funcs.map(Func::jvmMethod))
    withMethod(Main(body, globalScope))
  }.let { prog ->
    fun JvmAsm.string(): String = instrs.joinToString(separator = "\n", postfix = "\n") {
      val margin = when (it) {
        is JvmDirective, is JvmStringInstr, is JvmLabel -> "  "
        else -> "    "
      }
      margin + it.code
    } + if (methods.isEmpty()) "" else methods.joinToString(separator = "\n\n", transform = JvmAsm::string)
    prog.string()
  }
}

// <func>
data class Func(val retType: Type, val ident: Ident, val params: List<Variable>, val stat: Stat, val scope: Scope) {

  fun armAsm() = ARMAsm.write {
    val label = AsmLabel("f_" + ident.name)
    val statCode = stat.instr()
    data { +statCode.data }
    +label
    +PUSHInstr(LR)
    +statCode.instr
    +POPInstr(PC)
    +AsmDirective.ltorg

    withFunctions(statCode.funcs)
  }

  @JvmGenOnly
  val jvmMethod by lazy {
    DefinedMethod(ident.name, params.map { it.type.toJvm() }, retType.toJvm(), scope, stat)
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
