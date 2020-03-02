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
