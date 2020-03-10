package ic.org.jvm

import ic.org.util.Position

sealed class JvmDirective : JvmInstr

object SuperObject : JvmDirective() {
  override val code = ".super java/lang/Object"
}

data class Line(val i: Int) : JvmDirective() {
  constructor(pos: Position): this (pos.l)
  override val code = ".line $i"
}

data class JvmSpecialDirective(val directive : String) : JvmDirective() {
  override val code = directive
}