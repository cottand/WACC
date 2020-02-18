package ic.org.arm

import arrow.core.None
import ic.org.util.Code
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

abstract class StdFunc {
    abstract val name: String

    abstract val msgTemplate: String

    abstract val instructions: PersistentList<Instr>

    //TODO: Add label, msg and body in abstract class
}

object PrintIntStdFunc : StdFunc() {
    override val name = "p_print_int"

    val label = Label(name)

    override val msgTemplate = "%.d\\0"

    val msg = StringData(msgTemplate, msgTemplate.length - 1)

    override val instructions = persistentListOf(
        label,
        PUSHInstr(LR),
        MOVInstr(rd=Reg(1), op2=Reg(0)),
        LDRInstr(Reg.ret, ImmEqualLabel(msg.label)),
        ADDInstr(None, false, Reg(0), Reg(0), 4),
        BLInstr("printf"),
        LDRInstr(Reg.ret, 0),
        BLInstr("fflush"),
        POPInstr(PC)
    )

    val body = Code(instructions, msg.body)
}

object PrintStringStdFunc : StdFunc() {
    override val name = "p_print_string"

    val label = Label(name)

    override val msgTemplate = "%.*s\\0"

    val msg = StringData(msgTemplate, msgTemplate.length - 1)

    override val instructions = persistentListOf(
        label,
        PUSHInstr(LR),
        LDRInstr(Reg(1), Reg(0).zeroOffsetAddr),
        ADDInstr(None, false, Reg(2), Reg(0), 4),
        LDRInstr(Reg(0), ImmEqualLabel(msg.label)),
        ADDInstr(None, false, Reg(0), Reg(0), 4),
        BLInstr("printf"),
        LDRInstr(Reg.ret, 0),
        BLInstr("fflush"),
        POPInstr(PC)
    )

    val body = Code(instructions, msg.body)
}