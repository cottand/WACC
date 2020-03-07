package wacc.lang

data class Pair<F, S>(var fst: F,var snd: S) {
  override fun toString() = "Pair@" + java.lang.System.identityHashCode(this)
}
