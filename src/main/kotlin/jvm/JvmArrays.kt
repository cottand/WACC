package ic.org.jvm

import ic.org.util.NOT_REACHED


data class NEWARRAY(val type: JvmType) : JvmInstr {
  private val fullTypeRep = when(type) {
    is JvmInt -> "int"
    is JvmBool -> "bool"
    is JvmChar -> "char"
    else -> NOT_REACHED()
  }
  override val code = "newarray $fullTypeRep"
}

data class ANEWARRAY(val type: JvmType) : JvmInstr {
  private val fullTypeRep = type.rep.substring(1, type.rep.length - 1)
  override val code = "anewarray $fullTypeRep"
}