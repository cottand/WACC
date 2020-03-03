package ic.org.jvm

data class JvmSystemPrintFunc(val type: JvmType) : JvmMethod() {
  override val descriptor = "java/io/PrintStream/print"
  override val args = listOf(type)
  override val ret = JvmVoid
  private val instanceType by lazy { JvmType.inline("Ljava/io/PrintStream;") }
  override val invoke by lazy {
    JvmAsm {
      +GetStatic(JvmSystemOut, instanceType)
      +SWAP
      +InvokeVirtual(spec)
    }
  }
}

data class JvmSystemPrintlnFunc(val type: JvmType) : JvmMethod() {
  override val descriptor = "java/io/PrintStream/println"
  override val args = listOf(type)
  override val ret = JvmVoid
  private val instanceType by lazy { JvmType.inline("Ljava/io/PrintStream;") }
  override val invoke by lazy {
    JvmAsm {
      +GetStatic(JvmSystemOut, instanceType)
      +SWAP
      +InvokeVirtual(spec)
    }
  }
}

object JvmSystemOut : JvmField() {
  override val name = "java/lang/System/out"
}
