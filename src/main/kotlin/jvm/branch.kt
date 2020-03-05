package ic.org.jvm

data class GOTO(val label: JvmLabel) : JvmInstr {
  override val code = "goto ${label.name}"
}

data class IFEQ(val label: JvmLabel) : JvmInstr {
  override val code = "ifeq ${label.name}"
}

data class IFGE(val label: JvmLabel) : JvmInstr {
  override val code = "ifge ${label.name}"
}

data class IFGT(val label: JvmLabel) : JvmInstr {
  override val code = "ifgt ${label.name}"
}

data class IFLE(val label: JvmLabel) : JvmInstr {
  override val code = "ifle ${label.name}"
}

data class IFLT(val label: JvmLabel) : JvmInstr {
  override val code = "iflt ${label.name}"
}

data class IFNE(val label: JvmLabel) : JvmInstr {
  override val code = "ifne ${label.name}"
}

data class IF_ICMPEQ(val label: JvmLabel) : JvmInstr {
  override val code = "if_icmpeq ${label.name}"
}

data class IF_ICMPNE(val label: JvmLabel) : JvmInstr {
  override val code = "if_icmpne ${label.name}"
}

data class IF_ICMPGE(val label: JvmLabel) : JvmInstr {
  override val code = "if_icmpge ${label.name}"
}

data class IF_ICMPGT(val label: JvmLabel) : JvmInstr {
  override val code = "if_icmpgt ${label.name}"
}

data class IF_ICMPLE(val label: JvmLabel) : JvmInstr {
  override val code = "if_icmple ${label.name}"
}

data class IF_ICMPLT(val label: JvmLabel) : JvmInstr {
  override val code = "if_icmplt ${label.name}"
}
