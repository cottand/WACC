package ic.org.instr

import ic.org.arm.*
import ic.org.ast.Prog
import ic.org.util.Instructions
import ic.org.util.flatten
import kotlinx.collections.immutable.plus

fun Prog.instr(): Instructions = (body.instr() + funcs.map { it.instr() }.flatten())
  .let { (instrs, datas) ->
    DataSegment +
      datas +
      // .text??
      CodeSegment +
      Label("main") +
      PUSHInstr(LR) +
      instrs +
      LDRInstr(Reg(0), ImmEquals(0)) +
      POPInstr(PC)
  }
