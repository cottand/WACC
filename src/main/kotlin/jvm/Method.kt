package ic.org.jvm

import ic.org.ast.Stat

sealed class JvmMethod {
  internal abstract val descriptor: String
  internal abstract val args: List<JvmType>
  internal abstract val ret: JvmType
  internal val spec by lazy { "$descriptor(${args.joinToString(separator = "")})$ret" }
  val call by lazy { JvmAsm.instr(JvmDirective.inline("invokestatic $spec")) }
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
 * A methid defined in the WACC language
 */
open class DefinedMethod(
  override val descriptor: String,
  override val args: List<JvmType>,
  override val ret: JvmType,
  open val body: Stat
) : WACCMethod() {
  override val asm = JvmAsm {
    val maxStack = 20 // TODO change
    val locals = body.scope.localVarsSoFar()
    +JvmDirective.inline(".method public static $spec")
    +JvmDirective.inline(".limit stack $maxStack")
    +JvmDirective.inline(".limit locals $locals")
    +body.jvmInstr()
    +JvmDirective.inline(".end method")
  }
}

data class Main(override val body: Stat) : DefinedMethod("main", listOf(JvmArray(JvmString)), JvmVoid, body)

