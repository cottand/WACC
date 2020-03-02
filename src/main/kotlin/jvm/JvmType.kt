package ic.org.jvm

sealed class JvmType {
  abstract val rep: String
  override fun toString() = rep
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