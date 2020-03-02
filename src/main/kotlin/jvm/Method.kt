package ic.org.jvm

import ic.org.ast.Stat

sealed class JvmMethod {
  abstract val body: Stat
  abstract val name: String
  abstract val args: List<JvmType>
  abstract val ret: JvmType

  val asm = JvmAsm {
    val maxStack = 20 // TODO change
    val locals = body.scope.localVarsSoFar()
    +JvmDirective.inline(".method public static $name(${args.joinToString(separator = "")})$ret")
    +JvmDirective.inline(".limit stack $maxStack")
    +JvmDirective.inline(".limit locals $locals")
    TODO("Body")
    +JvmDirective.inline(".end method")
  }
}

data class Method(
  override val name: String,
  override val args: List<JvmType>,
  override val ret: JvmType,
  override val body: Stat
) :
  JvmMethod()

data class Main(override val body: Stat) : JvmMethod() {
  override val name = "main"
  override val args = listOf(JvmArray(JvmString))
  override val ret = JvmVoid
}

