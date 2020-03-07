package ic.org.jvm

object JvmNewPair : JvmInstr {
  override val code = "new stdlib/Pair"
}

object JvmNewPairInit : JvmMethod() {
  override val descriptor = "stdlib/Pair/<init>"
  override val args = listOf(JvmObject, JvmObject)
  override val ret = JvmVoid
}

object JvmGetFst : JvmInstr {
  override val code = "getfield stdlib/Pair/fst Ljava/lang/Object;"
}

object JvmGetSnd : JvmInstr {
  override val code = "getfield stdlib/Pair/snd Ljava/lang/Object;"
}