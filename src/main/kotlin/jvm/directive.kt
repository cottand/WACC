package ic.org.jvm

import ic.org.util.Position

sealed class JvmDirective : JvmInstr

object SuperObject : JvmInstr {
  override val code = ".super java/lang/Object"
}

data class Line(val i: Int) : JvmInstr {
  constructor(pos: Position): this (pos.l)
  override val code = ".line $i"
}
