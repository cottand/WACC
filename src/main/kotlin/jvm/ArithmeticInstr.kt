package ic.org.jvm

sealed class ArithmeticInstr(override val code: String) : JvmInstr

object INEG : ArithmeticInstr("ineg")

object LNEG : ArithmeticInstr("lneg")

object IADD : ArithmeticInstr("iadd")

object LADD : ArithmeticInstr("ladd")

object ISUB : ArithmeticInstr("isub")

object LSUB : ArithmeticInstr("lsub")

object IMUL : ArithmeticInstr("imul")

object LMUL : ArithmeticInstr("lmul")

object IDIV : ArithmeticInstr("idiv")

object IREM : ArithmeticInstr("irem")

object ARRAYLENGTH : ArithmeticInstr("arraylength")
