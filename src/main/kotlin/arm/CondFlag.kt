package ic.org.arm

import arrow.core.Option

sealed class CondFlag : Printable

typealias Flag = Option<CondFlag>

object EQCond : CondFlag() { // Equal
  override val code = "EQ"
}

object NECond : CondFlag() { // Not Equal
  override val code = "NE"
}

object CSCond : CondFlag() { // Carry Set
  override val code = "CS"
}

object VSCond : CondFlag() { // Overflow
  override val code = "VS"
}

object GECond : CondFlag() { // Signed greater or equal
  override val code = "GE"
}

object LTCond : CondFlag() { // Signed less than
  override val code = "LT"
}

object GTCond : CondFlag() { // Signed greater than
  override val code = "GT"
}

object LECond : CondFlag() { // Signed less than or equal
  override val code = "LE"
}
