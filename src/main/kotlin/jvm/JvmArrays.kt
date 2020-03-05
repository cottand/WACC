package ic.org.jvm

import ic.org.util.NOT_REACHED


data class NEWARRAY(val type: JvmType) : JvmInstr {
  private val fullTypeRep = when(type) {
    is JvmInt -> "int"
    is JvmBool -> "boolean"
    is JvmChar -> "char"
    is JvmArray -> type.rep
    else -> type.rep.substring(1, type.rep.length - 1)
  }
  override val code = (if(type is JvmArray) "anewarray" else "newarray") + " $fullTypeRep"
}