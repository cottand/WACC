package ic.org.jvm

object DUP : JvmInstr {
    override val code = "dup"
}

object DUP2 : JvmInstr {
    override val code = "dup2"
}

object POP : JvmInstr {
    override val code = "pop"
}

object POP2 : JvmInstr {
    override val code = "pop2"
}

object SWAP : JvmInstr {
    override val code = "swap"
}

// Swap2 is equivalent to dup2_x2 then pop_2
object SWAP2 : JvmInstr {
    override val code = "${DUP2_X2.code}\n${POP2.code}"
}

object DUP_X1 : JvmInstr {
    override val code = "dup_x1"
}

object DUP2_X2 : JvmInstr {
    override val code = "dup2_x2"
}

