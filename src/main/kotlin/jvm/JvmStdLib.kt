package ic.org.jvm

object InScanner : JvmField("wacc/lang/Utils/inScanner", Scanner)

data class JvmSystemPrintFunc(val type: JvmType) : JvmMethod(type = Virtual) {
  override val `class` = "java/io/PrintStream"
  override val mName = "print"
  override val args = listOf(type)
  override val ret = JvmVoid
  override val prelude = JvmAsm {
    +JvmSystemOut.getStatic
    +SWAP
  }
}

data class JvmSystemPrintlnFunc(val type: JvmType) : JvmMethod(type = Virtual) {
  override val `class` = "java/io/PrintStream"
  override val mName = "println"
  override val args = listOf(type)
  override val ret = JvmVoid
  override val prelude = JvmAsm {
    +JvmSystemOut.getStatic
    +SWAP
  }
}

object JvmSystemOut : JvmField("java/lang/System/out", PrintStream)

sealed class JvmSystemReadFunc : JvmMethod(type = Virtual) {
  override val `class` = "java/util/Scanner"
  override val args = listOf<Nothing>()
  abstract val post: JvmAsm
  override val prelude = JvmAsm { +InScanner.getStatic }
  override val invoke by lazy { prelude + Virtual.invoker(spec) + post }
}

object JvmReadInt : JvmSystemReadFunc() {
  override val mName = "nextInt"
  override val ret = JvmInt
  override val post = JvmAsm.empty
}

object JvmReadChar : JvmSystemReadFunc() {
  override val mName = "next"
  override val ret = JvmString
  override val post by lazy {
    JvmAsm {
      +LDC(0)
      +InvokeVirtual("java/lang/String/charAt(I)C")
    }
  }
}

object JvmReadString : JvmSystemReadFunc() {
  override val mName = "nextLine"
  override val ret = JvmString
  override val post = JvmAsm.empty
}

object JvmReadCharArray : JvmSystemReadFunc() {
  override val mName = "nextLine"
  override val ret = JvmString
  override val post by lazy {
    JvmAsm {
      +InvokeVirtual("java/lang/String/toCharArray()[C")
    }
  }
}
