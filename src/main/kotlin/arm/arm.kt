package ic.org.arm

sealed class ARMInstr {
  /**
   * Returns the ARM assembly code for the instruction
   */
  abstract fun code() : String

  /**
   * Aliases to ARMInstr::code()
   */
  override fun toString(): String {
    return code()
  }
}