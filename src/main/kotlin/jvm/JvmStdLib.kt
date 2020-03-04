package ic.org.jvm

import ic.org.util.shortRandomUUID

data class JvmSystemPrintFunc(val type: JvmType) : JvmMethod() {
  override val descriptor = "java/io/PrintStream/print"
  override val args = listOf(type)
  override val ret = JvmVoid
  private val instanceType by lazy { PrintStream }
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
  private val instanceType by lazy { PrintStream }
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

object JvmSystemIn : JvmField() {
  override val name = "java/lang/System/in Ljava/io/InputStream"
}

abstract class JvmSystemReadFunc(val delimiters: CharArray) : WACCMethod() {
  override val args = listOf(JvmVoid)
  abstract val body: JvmAsm

  val maxStack = 10 // TODO change to lower?
  val locals = 10 // TODO change to lower
  val retIndex = 1
  private val currIndex = 2
  private val nextCharLabel = JvmLabel("next_char_" + shortRandomUUID())
  private val retLabel = JvmLabel("return_" + shortRandomUUID())

  override val asm = JvmAsm.write {
    +".method public static $spec"
    +".limit stack $maxStack"
    +".limit locals $locals"
    +LDC(0)
    +ISTORE(retIndex)
    +nextCharLabel.code
    +GetStatic(JvmSystemIn, InputStream)
    +InvokeVirtual("java/io/InputStream/read()")
    +ISTORE(currIndex)
    +ILOAD(currIndex)
    +delimit()
    +parseChar()
    +GOTO(nextCharLabel)
    +retLabel.code
    if (ret is JvmVoid) {
      +LDC(0)
      +JvmReturn
    } else {
      +ILOAD(retIndex)
    }
    +".end method"
  }

  private fun delimit() = delimiters.asSequence().map {
    JvmAsm.write {
      +ILOAD(currIndex)
      +LDC(it.toInt())
      +ISUB
      +IFEQ(retLabel)
    }
  }.toList()

  abstract fun parseChar() : JvmAsm
}
