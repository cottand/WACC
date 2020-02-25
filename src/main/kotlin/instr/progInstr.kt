package ic.org.instr

import ic.org.arm.*
import ic.org.ast.Func
import ic.org.ast.Prog
import ic.org.util.Code
import ic.org.util.Instructions
import ic.org.util.print
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList

fun Prog.instr(): Instructions = body.instr()
  .withFunctions(funcs.map { it.instr() })
  .let {
    val allFuncs = it.funcs.fold(Code.empty, Code::combine)
    val allData = it.data + allFuncs.data
    val dataSegment = if (allData.isNotEmpty()) Directive.data + allData else persistentListOf<Nothing>()
    val (initScope, endScope) = globalScope.makeInstrScope()
    dataSegment +
      Directive.text +
      Directive.main +
      Label("main") +
      PUSHInstr(LR) +
      initScope +
      it.instr +
      endScope +
      LDRInstr(Reg(0), 0) +
      POPInstr(PC) +
      Directive.ltorg +
      // Function code segments:
      allFuncs.instr
  }
