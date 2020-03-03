package ic.org.jvm

data class ILOAD(val varIndex: Int) : JvmInstr {
  override val code = "iload $varIndex"
}

data class ISTORE(val varIndex: Int) : JvmInstr {
  override val code = "istore $varIndex"
}

data class ASTORE(val varIndex: Int) : JvmInstr {
  override val code = "astore_$varIndex"
}

data class ALOAD(val varIndex: Int) : JvmInstr {
  override val code = "aload_$varIndex"
}

sealed class LDC : JvmInstr {
  abstract val value: Any
  override val code by lazy { "ldc $value" }

  data class LDCString(override val value: String) : LDC()
  data class LDCInt(override val value: Int) : LDC()

  companion object {
    operator fun invoke(constant: Int) = LDCInt(constant)
    operator fun invoke(constant: String) = LDCString('"'+constant+'"')
    operator fun invoke(constant: Char) = LDCInt(constant.toInt())
  }
}



