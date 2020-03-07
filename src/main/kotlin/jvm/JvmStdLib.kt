package ic.org.jvm

import com.sun.org.apache.bcel.internal.generic.IRETURN
import ic.org.JVM
import ic.org.ast.GlobalScope
import ic.org.ast.Scope
import ic.org.util.shortRandomUUID
import javax.swing.ImageIcon

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

object JvmInputStreamReadChar : JvmMethod() {
  override val descriptor = "java/io/InputStream/read"
  override val args = listOf<Nothing>()
  override val ret = JvmChar
}

object JvmInputStreamReadInt : JvmMethod() {
  override val descriptor = "java/io/InputStream/read"
  override val args = listOf<Nothing>()
  override val ret = JvmInt
}

sealed class JvmSystemReadFunc() : WACCMethod() {
  override val args = listOf<Nothing>()
  abstract val scope: Scope

  val maxStack = 10 // TODO change to lower?
  val locals = 10 // TODO change to lower
  protected val retIndex = 1
  protected val currIndex = 2
  private val nextCharLabel = JvmLabel("next_char_" + shortRandomUUID())
  private val retLabel = JvmLabel("return_" + shortRandomUUID())
  private val delimiters = charArrayOf(' ')

  override val asm by lazy {
    JvmAsm.write {
      +".method public static $spec"
      +".limit locals $locals"
      +".limit stack $maxStack"
      +LDC(0)
      +ISTORE(retIndex)
      +nextCharLabel.code
      +GetStatic(JvmSystemIn, InputStream)
      +InvokeVirtual("java/io/InputStream/read()I")
      +ISTORE(currIndex)
//      +ILOAD(currIndex) //TODO may want to load here but decrease stack later?
      +delimit()
      +parseChar()
      +GOTO(nextCharLabel)
      +retLabel.code
      +ILOAD(retIndex)
      +ret.jvmReturn
      +".end method"
    }
  }

  private fun delimit() = delimiters.asSequence().map {
    JvmAsm.write {
      +ILOAD(currIndex)
      +LDC(it.toInt())
      +ISUB
      +IFEQ(retLabel)
    }
  }.toList()

  override val invoke by lazy { JvmAsm.instr(InvokeStatic("${scope.progName}/$spec")) }

  abstract fun parseChar(): JvmAsm
}

open class JvmReadChar(override val scope: Scope) : JvmSystemReadFunc() {
  override fun parseChar() = JvmAsm.empty
  override val descriptor = "readChar"
  override val ret = JvmChar
}

open class JvmReadInt(override val scope: Scope) : JvmSystemReadFunc() {
  override val descriptor = "readInt"
  override val args = listOf<Nothing>()

  //TODO: add support for negative integers
  override fun parseChar() = JvmAsm.write {
    +ILOAD(currIndex)
    +LDC('0'.toInt())
    +ISUB
    +LDC(10)
    +ILOAD(retIndex)
    +IMUL
    +IADD
    +ISTORE(retIndex)
  }

  override val ret = JvmInt
}
