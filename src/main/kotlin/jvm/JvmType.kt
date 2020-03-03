package ic.org.jvm

abstract class JvmType {
  abstract val rep: String
  override fun toString() = rep
  companion object {
    fun inline(rep: String) = object : JvmType() {
      override val rep = rep
    }
  }
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