package ic.org.jvm

import ic.org.ast.Scope
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
  override val name = "java/lang/System/in"
}

object JvmInputStreamRead : JvmMethod() {
  override val descriptor = "java/io/InputStream/read"
  override val args = listOf<Nothing>()
  override val ret = JvmInt
}

sealed class JvmSystemReadFunc() : WACCMethod() {
  override val args = listOf<Nothing>()
  abstract val scope: Scope
  override val invoke by lazy { JvmAsm.instr(InvokeStatic("${scope.progName}/$spec")) }

  val maxStack = 10 // TODO change to lower?
  val maxLocals = 3 // TODO change to lower

  protected val locals = (0 until maxLocals).iterator()
  protected val retLocal = locals.next()
  protected val currLocal = locals.next()

  protected val nextCharLabel = JvmLabel("next_char_" + shortRandomUUID())
  protected val retLabel = JvmLabel("return_" + shortRandomUUID())

  private val delimiters = charArrayOf(' ', '\n')

  override val asm by lazy {
    JvmAsm.write {
      +".method public static $spec"
      +".limit locals $maxLocals"
      +".limit stack $maxStack"
      +pre()
      +LDC(0)
      +ISTORE(retLocal)
      +nextCharLabel.code
      +GetStatic(JvmSystemIn, InputStream)
      +InvokeVirtual(JvmInputStreamRead)
      +ISTORE(currLocal)
      +delimit()
      +ILOAD(currLocal)
      +parse()
      +ISTORE(retLocal)
      +GOTO(nextCharLabel)
      +retLabel.code
      +ILOAD(retLocal)
      +post()
      +ret.jvmReturn
      +".end method"
    }
  }

  // initial setup of any relevant variables
  open fun pre() : JvmAsm = JvmAsm.empty

  // ..., value1 -> ..., value2
  open fun parse(): JvmAsm = JvmAsm.empty

  // ..., value1 -> ..., value2
  open fun post() : JvmAsm = JvmAsm.empty

  private fun delimit() = delimiters.asSequence().map {
    JvmAsm.write {
      +ILOAD(currLocal)
      +LDC(it.toInt())
      +ISUB
      +IFEQ(retLabel)
    }
  }.toList()

}

open class JvmReadChar(override val scope: Scope) : JvmSystemReadFunc() {
  override val descriptor = "readChar"
  override val ret = JvmChar
}

open class JvmReadInt(override val scope: Scope) : JvmSystemReadFunc() {
  override val descriptor = "readInt"
  override val args = listOf<Nothing>()
  override val ret = JvmInt

  private val signLocal = locals.next()
  private val skipSignLabel = JvmLabel("sign_" + shortRandomUUID())

  // assume integer is non-negative
  override fun pre() = JvmAsm {
    +LDC(1)
    +ISTORE(signLocal)
  }

  override fun parse() = JvmAsm.write {
    //check for sign
    +LDC('-'.toInt())
    +ISUB
    +IFNE(skipSignLabel)
    +LDC(-1)
    +ISTORE(signLocal)
    +GOTO(nextCharLabel)
    +skipSignLabel.code

    // parse integer by each digit
    +ILOAD(currLocal)
    +LDC('0'.toInt())
    +ISUB
    +LDC(10)
    +ILOAD(retLocal)
    +IMUL
    +IADD
  }

  override fun post() = JvmAsm {
    +ILOAD(signLocal)
    +IMUL
  }
}
