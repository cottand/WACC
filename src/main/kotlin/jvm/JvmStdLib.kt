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

object JvmInputStreamRead : JvmMethod(type = Virtual) {
  override val `class` = "java/io/InputStream"
  override val mName = "read"
  override val args = listOf<Nothing>()
  override val ret = JvmInt
}

sealed class JvmSystemReadFunc() : WACCMethod(type = Static) {
  override val args = listOf<Nothing>()
  abstract val scope: Scope
  val maxStack = 10 // TODO change to lower?
  val maxLocals = 3

  protected val locals = (0 until maxLocals).iterator()
  protected val retLocal = locals.next()
  protected val currLocal = locals.next()

  protected val nextCharLabel = JvmLabel("next_char")
  protected val retLabel = JvmLabel("return")

  private val delimiters = charArrayOf(' ', '\n')

  override val asm by lazy {
    JvmAsm.write {
      +".method public static $header"
      +".limit locals $maxLocals"
      +".limit stack $maxStack"
      +pre()
      +LDC(0)
      +ISTORE(retLocal)
      +nextCharLabel.code
      +GetStatic(JvmSystemIn, JvmInputStream)
      +JvmInputStreamRead.invoke
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

class JvmReadChar(override val scope: Scope) : JvmSystemReadFunc() {
  override val `class` = scope.progName
  override val mName = "readChar"
  override val ret = JvmChar
}

class JvmReadInt(override val scope: Scope) : JvmSystemReadFunc() {
  override val `class` = scope.progName
  override val mName = "readInt"
  override val args = listOf<Nothing>()
  override val ret = JvmInt

  private val signLocal = locals.next()
  private val skipSignLabel = JvmLabel("sign")

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
