package ic.org.jvm

import arrow.core.toOption
import ic.org.ast.GlobalScope
import ic.org.ast.Scope
import ic.org.ast.Stat

abstract class JvmMethod(type: MethodType) {
  internal abstract val `class`: String?
  internal abstract val mName: String
  internal abstract val args: List<JvmType>
  internal abstract val ret: JvmType

  /**
   * What shows in function declaration
   */
  internal val header by lazy {
    "$mName(${args.joinToString(separator = "") { it.rep }})$ret"
  }

  /**
   * What shows in function calling
   */
  internal val spec by lazy {
    val pre = `class`.toOption().fold({ "" }, { "$it/" })
    pre + header
  }
  open val invoke by lazy { prelude + type.invoker(spec) }
  open val prelude = JvmAsm.empty
}

sealed class MethodType(val invoker: (String) -> JvmInstr)
object Static : MethodType(::InvokeStatic)
object Virtual : MethodType(::InvokeVirtual)
object Special : MethodType(::InvokeSpecial)

object JvmSystemExit : JvmMethod(type = Static) {
  override val `class` = "java/lang/System"
  override val mName = "exit"
  override val args = listOf(JvmInt)
  override val ret = JvmVoid
}

/**
 * A WACC method that has a body that compiles to bytecode
 */
abstract class WACCMethod(type: MethodType) : JvmMethod(type) {
  abstract val asm: JvmAsm
}

/**
 * A method defined in the WACC language.
 *
 * Caller must do LDC and JvmReturn to return stuff
 */
open class DefinedMethod(
  override val mName: String,
  override val args: List<JvmType>,
  override val ret: JvmType,
  scope: Scope,
  open val body: Stat? = null
) : WACCMethod(type = Static) {
  override val `class` = scope.progName
  override val asm by lazy {
    JvmAsm {
      val maxStack = 100
      val locals = body?.scope?.totalLocalVarsSoFar()
      +".method public static $header"
      +".limit stack $maxStack"
      +".limit locals ${(locals ?: 1) + 2}"
      body?.let { +it.jvmInstr() }
      if (ret is JvmVoid) {
        +LDC(0)
        +JvmReturn.Void
      }
      +".end method"
    }
  }
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
    JvmInt, JvmChar, JvmBool -> JvmReturn.Int
    is JvmArray, JvmObject, JvmString, PrintStream, JvmWaccPair, JvmInputStream -> JvmReturn.Object
    JvmVoid -> JvmReturn.Void
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
}

data class InvokeSpecial(val spec: String) : JvmInstr {
  override val code = "invokespecial $spec"
}