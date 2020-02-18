package ic.org.arm

import arrow.core.None
import ic.org.util.Code
import kotlinx.collections.immutable.persistentListOf

abstract class StdFunc {
    abstract val name: String
    abstract val body: Code
    val label = Label(name)
}

object PrintStdFunc : StdFunc() {
    override val name = "p_print_int"

    private const val format = "%.d\\0"
    private val msg1 = StringData(format, format.length - 1)

    private val instructions = persistentListOf(
        label,
        PUSHInstr(LR),
        LDRInstr(Reg(1), Reg(0).zeroOffsetAddr),
        ADDInstr(None, false, Reg(2), Reg(0), 4),
        LDRInstr(Reg(0), ImmEqualLabel(msg1.label)),
        ADDInstr(None, false, Reg(0), Reg(0), 4),
        BLInstr("printf"),
        LDRInstr(Reg.ret, 0),
        BLInstr("fflush"),
        POPInstr(PC)
    )
    override val body = Code(instructions, msg1.body)
}

/*
p_print_int:
30		PUSH {lr}
31		MOV r1, r0
32		LDR r0, =msg_1
33		ADD r0, r0, #4
34		BL printf
35		MOV r0, #0
36		BL fflush
37		POP {pc}*/