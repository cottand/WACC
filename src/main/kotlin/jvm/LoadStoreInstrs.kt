package ic.org.jvm

import kotlin.reflect.jvm.internal.impl.metadata.jvm.deserialization.JvmProtoBufUtil

data class ILOAD(val varIndex: Int) : JvmInstr {
  override val code = "iload $varIndex"
}

data class ISTORE(val varIndex: Int) : JvmInstr {
  override val code = "istore $varIndex"
}

data class ASTORE(val varIndex: Int? = null, val type: JvmType = JvmObject) : JvmInstr {
  override val code = when(type) {
    is JvmInt -> "i"
    is JvmChar -> "c"
    is JvmBool -> "b"
    is JvmArray -> "a"
    else -> ""
  } + if (varIndex == null)  "astore" else "astore_$varIndex"}

data class ALOAD(val varIndex: Int? = null, val type: JvmType = JvmObject) : JvmInstr {
  override val code = when(type) {
    is JvmInt -> "i"
    is JvmChar -> "c"
    is JvmBool -> "b"
    is JvmArray -> "a"
    else -> ""
  } + if (varIndex == null)  "aload" else "aload_$varIndex"
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

data class BIPUSH(val byte: Byte) : JvmInstr {
  override val code = "bipush $byte"
}

object ACONST_NULL : JvmInstr {
  override val code = "aconst_null"
}