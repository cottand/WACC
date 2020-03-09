package ic.org.jvm

import ic.org.ast.GlobalScope
import ic.org.ast.Scope
import ic.org.ast.Stat

abstract class JvmMethod {
  internal abstract val descriptor: String
  internal abstract val args: List<JvmType>
  internal abstract val ret: JvmType
  internal val spec by lazy { "$descriptor(${args.joinToString(separator = "") { it.rep }})$ret" }
  open val invoke by lazy { JvmAsm.instr(InvokeStatic(spec)) }
}

object JvmSystemExit : JvmMethod() {
  override val descriptor = "java/lang/System/exit"
  override val args = listOf(JvmInt)
  override val ret = JvmVoid
}

/**
 * A WACC method that has a body that compiles to bytecode
 */
abstract class WACCMethod : JvmMethod() {
  abstract val asm: JvmAsm
}

/**
 * A method defined in the WACC language.
 *
 * Caller must do LDC and JvmReturn to return stuff
 */
open class DefinedMethod(
  override val descriptor: String,
  override val args: List<JvmType>,
  override val ret: JvmType,
  scope: Scope,
  open val body: Stat? = null
) : WACCMethod() {
  private val progName = scope.progName
  override val asm by lazy {
    JvmAsm {
      val maxStack = 20 // TODO change
      val locals = body?.scope?.localVarsSoFar()
      +".method public static $spec"
      +".limit stack $maxStack"
      +".limit locals ${(locals ?: 1) + 1}"
      body?.let { +it.jvmInstr() }
      if (ret is JvmVoid) {
        +LDC(0)
        +JvmReturn.Void
      }
      +".end method"
    }
  }
  override val invoke = JvmAsm.instr(InvokeStatic("$progName/$spec"))
}

data class Main(override val body: Stat, val gscope: GlobalScope) :
  DefinedMethod("main", listOf(JvmArray(JvmString)), JvmVoid, gscope, body)


object JvmReturnObj : JvmInstr {
  override val code = "areturn"
}

enum class JvmReturn(override val code: String) : JvmInstr {
  Void("return"),
  Object("areturn"),
  Int("ireturn")
}

val JvmType.jvmReturn
  get() = when (this) {
    JvmInt, JvmBool, JvmChar -> JvmReturn.Int
    is JvmArray -> TODO()
    JvmObject, JvmString, PrintStream, InputStream -> JvmReturn.Object
    JvmVoid -> JvmReturn.Void
    JvmBool -> TODO()
  }

abstract class JvmField {
  abstract val name: String
  override fun toString() = name
}

data class GetStatic(val staticField: JvmField, val type: JvmType) : JvmInstr {
  override val code = "getstatic $staticField $type"
}

data class InvokeStatic(val spec: String) : JvmInstr {
  override val code = "invokestatic $spec"
}

data class InvokeVirtual(val spec: String) : JvmInstr {
  override val code = "invokevirtual $spec"

  constructor(method: JvmMethod) : this(method.spec)
}

data class InvokeSpecial(val spec: String) : JvmInstr {
  override val code = "invokespecial $spec"

  constructor(method: JvmMethod) : this(method.spec)
}