package ic.org.jvm

import java.lang.RuntimeException
import kotlin.reflect.jvm.internal.impl.metadata.jvm.deserialization.JvmProtoBufUtil

data class ILOAD(val varIndex: Int) : JvmInstr {
  override val code = "iload $varIndex"
}

data class ISTORE(val varIndex: Int) : JvmInstr {
  override val code = "istore $varIndex"
}

data class ASTORE constructor(val varIndex: Int? = null, val type: JvmType = JvmObject) : JvmInstr {
  override val code = when(type) {
    is JvmInt -> "i"
    is JvmChar -> "c"
    is JvmBool -> "b"
    is JvmArray -> "a"
    else -> ""
  } + if (varIndex == null)  "astore" else "astore $varIndex"

}

data class ALOAD constructor(val varIndex: Int? = null, val type: JvmType = JvmObject) : JvmInstr {
  override val code = when(type) {
    is JvmInt -> "i"
    is JvmChar -> "c"
    is JvmBool -> "b"
    is JvmArray -> "a"
    else -> ""
  } + if (varIndex == null)  "aload" else "aload $varIndex"

  companion object {
    operator fun invoke(index: Int? = null, type: JvmType = JvmObject) = when(index) {
      null -> JvmAsm.instr(ALOAD(type = type))
      in 0..3 -> JvmAsm.instr(ALOAD(3, type))
      else -> JvmAsm {
        +LDC(index)
        +ALOAD(type = type)
      }
    }
  }
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