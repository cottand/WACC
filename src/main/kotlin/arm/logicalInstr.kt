package ic.org.arm

sealed class LogicalInstr : ARMInstr() {
  abstract val instr: String

  override fun code(): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
