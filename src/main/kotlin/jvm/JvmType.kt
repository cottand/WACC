package ic.org.jvm

import ic.org.ast.AnyArrayT
import ic.org.ast.AnyPairTs
import ic.org.ast.BoolT
import ic.org.ast.CharT
import ic.org.ast.IntT
import ic.org.ast.StringT
import ic.org.ast.Type

abstract class JvmType {
  abstract val rep: String
  override fun toString() = rep
  companion object {
    fun inline(rep: String) = object : JvmType() {
      override val rep = rep
    }
  }
}

fun Type.toJvm() = when(this) {
  IntT -> JvmInt
  BoolT -> JvmBool
  CharT -> JvmChar
  StringT -> JvmString
  is AnyArrayT -> TODO()
  is AnyPairTs -> TODO()
}

object JvmInt : JvmType() {
  override val rep = "I"
}

data class JvmArray(val t: JvmType) : JvmType() {
  override val rep = "[$t"
}

object Object : JvmType() {
  override val rep = "Ljava/lang/Object;"
}

object JvmString : JvmType() {
  override val rep = "Ljava/lang/String;"
}

object JvmVoid : JvmType() {
  override val rep = "V"
}

object JvmBool : JvmType() {
  override val rep = "Z"
}

object JvmChar : JvmType() {
  override val rep = "C"
}