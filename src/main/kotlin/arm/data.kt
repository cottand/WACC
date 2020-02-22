package ic.org.arm

import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

/**
 * Represents a unique label in a code segment that references a string.
 *
 * Same-name labels are allowed thanks to each label havin a unique [id].
 */
data class StringData(val msg: String, val length: Int) {
  val id = UUID.randomUUID().toString().take(8)
  val label = Label("s_$id")
  val body = persistentListOf(
    label,
    Directive("word $length"),
    Directive("ascii\t\"$msg\"")
  )
}