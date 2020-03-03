package ic.org.jvm

import ic.org.ast.Stat

abstract class JvmMethod {
  internal abstract val descriptor: String
  internal abstract val args: List<JvmType>
  internal abstract val ret: JvmType
  internal val spec by lazy { "$descriptor(${args.joinToString(separator = "") { it.rep }})$ret" }
  open val invoke by lazy { JvmAsm.instr(InvokeStatic(this)) }
}

object JvmSystemExit : JvmMethod() {
  override val descriptor = "java/lang/System/exit"
  override val args = listOf(JvmInt)
  override val ret = JvmVoid
}

/**
 * A WACC method that has a body that compiles to bytecode
 */
sealed class WACCMethod : JvmMethod() {
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
  open val body: Stat? = null
) : WACCMethod() {
  override val asm by lazy {
    JvmAsm {
      val maxStack = 20 // TODO change
      val locals = body?.scope?.localVarsSoFar()
      +".method public static $spec"
      +".limit stack $maxStack"
      +".limit locals ${(locals?:1) + 1}"
      body?.let { +it.jvmInstr() }
      if (ret is JvmVoid) {
        +LDC(0)
        +JvmReturn
      }
      +".end method"
    }
  }
}

data class Main(override val body: Stat) : DefinedMethod("main", listOf(JvmArray(JvmString)), JvmVoid, body)

object JvmReturn : JvmInstr {
  override val code = "return"
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
  constructor(method: JvmMethod) : this(method.spec)
}

data class InvokeVirtual(val spec: String) : JvmInstr {
  override val code = "invokevirtual $spec"
  constructor(method: JvmMethod) : this(method.spec)
}

data class InvokeSpecial(val spec: String) : JvmInstr {
  override val code = "invokespecial $spec"
  constructor(method: JvmMethod) : this(method.spec)
}