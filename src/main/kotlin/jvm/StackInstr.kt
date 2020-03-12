@file:Suppress("ClassName")

package ic.org.jvm

sealed class StackInstr(override val code: String) : JvmInstr

object DUP : StackInstr("dup")

object DUP2 : StackInstr("dup2")

object POP : StackInstr("pop")

object POP2 : StackInstr("pop2")

object SWAP : StackInstr("swap")

// Swap2 is equivalent to dup2_x2 then pop_2
object SWAP2 : StackInstr(DUP2_X2.code + '\n' + POP2.code)

object DUP_X1 : StackInstr("dup_x1")

object DUP2_X2 : StackInstr("dup2_x2")

