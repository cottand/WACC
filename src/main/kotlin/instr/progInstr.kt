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
      // LDRInstr(rd = Reg(0), addressing = AddrMode2()) TODO LDR r0, =0 how tf do you write that
      POPInstr(PC)
  }
