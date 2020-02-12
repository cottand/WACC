package ic.org.instr

import ic.org.arm.*
import ic.org.ast.Prog
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.plus

fun Prog.instr(): PersistentList<Instr> =
  DataSegment +
    CodeSegment +
    Label("main") +
    PUSHInstr(LR) +
    body.instr() +
    // LDRInstr(rd = Reg(0), addressing = AddrMode2()) TODO LDR r0, =0 how tf do you write that
    POPInstr(PC)
