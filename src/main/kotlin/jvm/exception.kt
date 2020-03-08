package ic.org.jvm

import ic.org.ast.expr.*
import ic.org.util.NOT_REACHED
import ic.org.util.shortRandomUUID

object ATHROW : JvmInstr {
  override val code = "athrow"
}

fun JvmCheckIntegerOverflow(op : BinaryOper) = JvmAsm {
  val msg = "Integer Overflow"
  val exception = "java/lang/RuntimeException"
  val method = "java/lang/RuntimeException/<init>(Ljava/lang/String;)V"
  val labelSkip = JvmLabel("L_OVERFLOW_SKIP_" + shortRandomUUID())

  +DUP2
  +DUP_X1
  +I2L
  +SWAP2
  +SWAP
  +POP
  +I2L

  +when (op) {
    is MulBO -> LMUL
    is PlusBO -> LADD
    is MinusBO -> LSUB
    else -> NOT_REACHED()
  }

  +LDC(2147483647)
  +I2L
  +LCMP
  +IFLE(labelSkip)
  +NEW(exception)
  +DUP
  +LDC(msg)
  +InvokeSpecial(method)
  +ATHROW
  +labelSkip
}
