package ic.org.jvm

data class JvmSystemPrintFunc(val type: JvmType) : JvmMethod(type = Virtual) {
  override val `class` = "java/io/PrintStream"
  override val mName = "print"
  override val args = listOf(type)
  override val ret = JvmVoid
  private val instanceType by lazy { PrintStream }
  override val prelude by lazy {
    JvmAsm {
      +GetStatic(JvmSystemOut, instanceType)
      +SWAP
    }
  }
}

data class JvmSystemPrintlnFunc(val type: JvmType) : JvmMethod(type = Virtual) {
  override val `class` = "java/io/PrintStream"
  override val mName = "println"
  override val args = listOf(type)
  override val ret = JvmVoid
  private val instanceType by lazy { PrintStream }
  override val prelude by lazy {
    JvmAsm {
      +GetStatic(JvmSystemOut, instanceType)
      +SWAP
    }
  }
}

object JvmSystemOut : JvmField() {
  override val name = "java/lang/System/out"
}
