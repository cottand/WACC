package ic.org.jvm

import ic.org.ast.expr.*
import ic.org.util.NOT_REACHED
import ic.org.util.shortRandomUUID

object ATHROW : JvmInstr {
  override val code = "athrow"
}

/**
 * Add JvmAsm code to check integer underflow/overflow for binary operators
 */
fun jvmCheckIntegerOverflowBO(op : BinaryOper) : JvmAsm {
  val stackManipulation = JvmAsm {
    +DUP2
    +DUP_X1
    +I2L
    +SWAP2
    +SWAP
    +POP
    +I2L
  }

  return when (op) {
    is MulBO -> jvmCheckIntegerOverflowGeneric(JvmAsm { +LMUL }, stackManipulation)
    is PlusBO -> jvmCheckIntegerOverflowGeneric(JvmAsm { +LADD }, stackManipulation)
    is MinusBO -> jvmCheckIntegerOverflowGeneric(JvmAsm { +LSUB }, stackManipulation)
    else -> NOT_REACHED()
  }
}

/**
 * Add JvmAsm code to check integer underflow/overflow for unary operators
 */
fun jvmCheckIntegerOverflowUO(op : UnaryOper) : JvmAsm {
  val stackManipulation = JvmAsm {
    +DUP
    +I2L
  }

  return when (op) {
    MinusUO -> jvmCheckIntegerOverflowGeneric(JvmAsm { +LNEG }, stackManipulation)
    else -> NOT_REACHED()
  }
}

/**
 * Generic function to generate JvmAsm to check for integer overflow/underflow. [oper] is the code that should
 * be used to perform the potentially overflowing operation. [stackManipulation] is any code that should be used
 * to perform stack manipulation such as duplicating values before using [oper] on them.
 */
fun jvmCheckIntegerOverflowGeneric(oper: JvmAsm, stackManipulation: JvmAsm) = JvmAsm {
  val msg = "Integer Overflow"
  val exception = "java/lang/RuntimeException"
  val method = "java/lang/RuntimeException/<init>(Ljava/lang/String;)V"
  val labelSkipOF = JvmLabel("L_OVERFLOW_SKIP_" + shortRandomUUID())
  val labelSkipUF = JvmLabel("L_UNDERFLOW_SKIP_" + shortRandomUUID())

  // Check for overflow
  +stackManipulation
  +oper

  +LDC(2147483647)
  +I2L
  +LCMP
  +IFLE(labelSkipOF)
  +NEW(exception)
  +DUP
  +LDC(msg)
  +InvokeSpecial(method)
  +ATHROW
  +labelSkipOF

  // Check for underflow
  +stackManipulation
  +oper

  +LDC(-2147483648)
  +I2L
  +LCMP
  +IFGE(labelSkipUF)
  +NEW(exception)
  +DUP
  +LDC(msg)
  +InvokeSpecial(method)
  +ATHROW
  +labelSkipUF
}
