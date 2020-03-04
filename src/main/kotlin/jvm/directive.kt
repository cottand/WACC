package ic.org.jvm

abstract class JvmDirective : JvmInstr {
  companion object {
    fun inline(txt: String) = object : JvmDirective() {
      override val code = txt
    }
  }
}

object SuperObject : JvmDirective() {
  override val code = ".super java/lang/Object"
}