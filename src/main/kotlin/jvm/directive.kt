package ic.org.jvm

import ic.org.util.Position

sealed class JvmDirective : JvmInstr {
  companion object {
  }
}

object SuperObject : JvmDirective() {
  override val code = ".super java/lang/Object"
}

data class Line(val i: Int) : JvmDirective() {
  constructor(pos: Position): this (pos.l)
  override val code = ".line $i"
}