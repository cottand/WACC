package ic.org.jvm

object INEG : JvmInstr {
  override val code = "ineg"
}

object IADD : JvmInstr {
  override val code = "iadd"
}

object LADD : JvmInstr {
  override val code = "ladd"
}

object ISUB : JvmInstr {
  override val code = "isub"
}

object LSUB : JvmInstr {
  override val code = "lsub"
}

object IMUL : JvmInstr {
  override val code = "imul"
}

object LMUL : JvmInstr {
  override val code = "lmul"
}

object IDIV : JvmInstr {
  override val code = "idiv"
}

object IREM : JvmInstr {
  override val code = "irem"
}

object ARRAYLENGTH : JvmInstr {
  override val code = "arraylength"
}