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
  .withFunctions(funcs.map(Func::instr))
  .addErrors()
  .let {
    val allData = it.data + it.functions.data
    val dataSegment = if (allData.isNotEmpty()) Directive.data + allData else persistentListOf<Nothing>()
    val (initScope, endScope) = body.scope.makeInstrScope()
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
      it.functions.instr

  }

/**
 * Traverses all the code in [Code] and looks for instructions that might throw exceptions, like [ADDInstr] and
 * overflows. For each unique one it finds, it adds the respective error handling functions to the program.
 */
fun Code.addErrors(): Code {
  var overflow = false
  var runtimeError = false
  // TODO more
  val program = instr.toMutableList()
  val iter = program.listIterator()
  for (line in iter) {
    if (line !is ARMInstr) continue
    when (line) {
      is ADDInstr, is SUBInstr, is MULInstr -> {
        iter.add(BLInstr(condFlag = VSCond, label = OverflowException.label))
        overflow = true
      }
      // TODO add more
    }
  }
  var newCode = Code(program.toPersistentList(), data).withFunction(functions)

  if (overflow) {
    newCode = newCode.withFunction(OverflowException.body)
    runtimeError = true
  }

  if (runtimeError)
    newCode = newCode.withFunction(RuntimeError.body)

  return newCode
}




