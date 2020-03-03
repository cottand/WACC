package ic.org.jvm

object JvmNewPair : JvmInstr {
  override val code = "new Pair"
}

object JvmNewPairInit : JvmMethod() {
  override val descriptor = "Pair/<init>"
  override val args = listOf(JvmObject, JvmObject)
  override val ret = JvmVoid
}

object JvmGetFst : JvmInstr {
  override val code = "getfield Pair/fst Ljava/lang/Object;"
}

object JvmGetSnd : JvmInstr {
  override val code = "getfield Pair/snd Ljava/lang/Object;"
}