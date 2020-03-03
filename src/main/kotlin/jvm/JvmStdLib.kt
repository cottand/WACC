package ic.org.jvm

sealed class JvmSystemPrintFunc(val type : JvmType) : JvmMethod() {
  override val descriptor = "java/io/PrintStream/print"
  override val args = listOf(type)
  override val ret = JvmVoid
  private val instanceType by lazy { "Ljava/io/PrintStream;" }
  override val invoke by lazy {
    JvmAsm {
      +"getstatic java/lang/System/out $instanceType"
      +"invokevirtual $spec"
    }
  }
}

