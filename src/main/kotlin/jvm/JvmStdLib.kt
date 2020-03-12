package ic.org.jvm

import ic.org.ast.Scope
import ic.org.util.shortRandomUUID

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

object JvmSystemIn : JvmField() {
  override val name = "java/lang/System/in"
}

sealed class JvmSystemReadFunc : JvmMethod(type = Virtual) {
  override val `class` = "java/util/Scanner"
  override val args = listOf<Nothing>()
  private val instanceType by lazy { JvmInputStream }
  abstract val post : JvmAsm
  override val prelude by lazy {
    JvmAsm {
      +NEW("java/util/Scanner")
      +DUP
      +GetStatic(JvmSystemIn, instanceType)
      +InvokeSpecial("java/util/Scanner/<init>(Ljava/io/InputStream;)V")
    }
  }
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
