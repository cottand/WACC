package wacc.lang

data class Pair<F, S>(val fst: F,val snd: S) {
  override fun toString() = "Pair@" + java.lang.System.identityHashCode(this)
}
