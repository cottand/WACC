package ic.org.arm

sealed class CondFlag : Printable

object EQCond : CondFlag() { // Equal
  override val code = "EQ"
}
object NECond : CondFlag() { // Not Equal
  override val code = "NE"
}
object HSCond : CondFlag() { // Unsigned higher or same
  override val code = "HS"
}
object CSCond : CondFlag() { // Carry Set
  override val code = "CS"
}
object LOCond : CondFlag() { // Unsigned lower
  override val code = "LO"
}
object CCCond : CondFlag() {// Carry clear
override val code = "CC"
}
object MICond : CondFlag() { // Negative, Minus
  override val code = "MI"
}
object PLCond : CondFlag() { // Positive or zero, plus
  override val code = "PL"
}
object VSCond : CondFlag() { // Overflow
  override val code = "VS"
}
object VCCond : CondFlag() { // No overflow
  override val code = "VC"
}
object HICond : CondFlag() { // Unsigned higher
  override val code = "HI"
}
object LSCond : CondFlag() { // Unsigned lower or same
  override val code = "LS"
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
object ALCond : CondFlag() { // Always
  override val code = "AL"
}
