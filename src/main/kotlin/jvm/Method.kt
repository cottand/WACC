package ic.org.jvm

sealed class JvmMethod {
  abstract fun body(): JvmAsm
  abstract val name: String
  abstract val args: List<JvmType>
  abstract val ret: JvmType
}

data class Method(override val name: String, override val args: List<JvmType>, override val ret: JvmType) :
  JvmMethod() {
  override fun body() = TODO()

}

