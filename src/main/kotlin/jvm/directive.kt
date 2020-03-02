package ic.org.jvm

sealed class JvmDirective : JvmInstr

object MainClass : JvmDirective() {
  override val code = ".class public wacc"
}

object SuperObject : JvmDirective() {
  override val code = ".super java/lang/Object"
}

