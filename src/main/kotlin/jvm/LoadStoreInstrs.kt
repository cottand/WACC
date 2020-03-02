package ic.org.jvm

data class ILOAD(val variable : LocalVar) : JvmInstr {
    override val code = "iload $variable"
}

data class ISTORE(val variable : LocalVar) : JvmInstr {
    override val code = "istore $variable"
}

data class LDC(val constant : Constant) : JvmInstr {
    override val code = "ldc $constant"
}

data class LocalVar(val index : Int) : JvmInstr {
    override val code = "$index"
}

data class Constant(val index : Int) : JvmInstr {
    override val code = "$index"
}