import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ic.org.containsLefts
import ic.org.containsRights
import ic.org.findFirstLeft
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

  @Test
  fun findFirstLeft() {
    val l: List<Either<Char, Int>> = listOf(1.right())
    val l2 = listOf(2.left())
    val l3 = l + l2
    assertEquals(listOf(1).right(),l.findFirstLeft())
    assertEquals(2.left(),l2.findFirstLeft())
    assertEquals(2.left(),l3.findFirstLeft())
  }
}