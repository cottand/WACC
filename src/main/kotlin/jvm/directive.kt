package ic.org.jvm

abstract class JvmDirective : JvmInstr {
  companion object {
    fun inline(txt: String) = object : JvmDirective() {
      override val code = txt
    }
  }
}

object MainClass : JvmDirective() {
  override val code = ".class public wacc"
}

object SuperObject : JvmDirective() {
  override val code = ".super java/lang/Object"
}
