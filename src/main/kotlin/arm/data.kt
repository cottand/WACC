package ic.org.arm

import kotlinx.collections.immutable.persistentListOf

data class StringData(val name: String, val msg: String, val length: Int) {
  val label = Label(name)
  val body = persistentListOf(
    label,
    Directive("word $length"),
    Directive("ascii \"$msg\"")
  )
}