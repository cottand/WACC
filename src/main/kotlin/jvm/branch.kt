@file:Suppress("ClassName")

package ic.org.jvm

sealed class BranchInstr(mnemonic: String, label: JvmLabel) : JvmInstr {
  override val code = "$mnemonic ${label.name}"
}

data class GOTO(val label: JvmLabel) : BranchInstr("goto", label)

data class IFEQ(val label: JvmLabel) : BranchInstr("ifeq", label)

data class IFGE(val label: JvmLabel) : BranchInstr("ifge", label)

data class IFGT(val label: JvmLabel) : BranchInstr("ifgt", label)

data class IFLE(val label: JvmLabel) : BranchInstr("ifle", label)

data class IFLT(val label: JvmLabel) : BranchInstr("iflt", label)

data class IFNE(val label: JvmLabel) : BranchInstr("ifne", label)

data class IF_ICMPEQ(val label: JvmLabel) : BranchInstr("if_icmpeq", label)

data class IF_ICMPNE(val label: JvmLabel) : BranchInstr("if_icmpne", label)

data class IF_ICMPGE(val label: JvmLabel) : BranchInstr("if_icmpge", label)

data class IF_ICMPGT(val label: JvmLabel) : BranchInstr("if_icmpgt", label)

data class IF_ICMPLE(val label: JvmLabel) : BranchInstr("if_icmple", label)

data class IF_ICMPLT(val label: JvmLabel) : BranchInstr("if_icmplt", label)

data class IF_ACMPEQ(val label: JvmLabel) : BranchInstr("if_acmpeq", label)

data class IF_ACMPNE(val label: JvmLabel) : BranchInstr("if_acmpne", label)

data class IFNONNULL(val label: JvmLabel) : BranchInstr("ifnonnull", label)

object LCMP : JvmInstr {
  override val code = "lcmp"
}
