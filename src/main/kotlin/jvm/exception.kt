package ic.org.jvm

import ic.org.ast.expr.BinaryOper
import ic.org.ast.expr.DivBO
import ic.org.ast.expr.MulBO

object ATHROW : JvmInstr {
  override val code = "athrow"
}

fun JvmCheckIntegerOverflow(op : BinaryOper) = JvmAsm {
  val msg = "Integer Overflow"
  val exception = "java/lang/RuntimeException/<init>(Ljava/lang/String;)V"

  when (op) {
    is MulBO -> TODO()
  }
}
