import arrow.core.left
import arrow.core.right
import ic.org.containsLefts
import ic.org.containsRights
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class UtilsKtTest {

  @Test
  fun containsEither() {
    val l = listOf(
      1.right(),
      2.right()
    )
    assertFalse(l.containsLefts())
    val l2 = listOf('h'.left())
    val l3 = l + l2
    assertFalse(l2.containsRights())
    assertTrue(l2.containsLefts())
    assertTrue(l3.containsLefts())
    assertTrue(l3.containsRights())
  }
}