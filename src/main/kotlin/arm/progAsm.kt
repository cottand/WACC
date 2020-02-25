package ic.org.arm

import ic.org.arm.instr.LDRInstr
import ic.org.arm.instr.POPInstr
import ic.org.arm.instr.PUSHInstr
import ic.org.ast.Prog
import ic.org.util.Code
import ic.org.util.Instructions
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus

fun Prog.asm(): Instructions = body.instr()
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
