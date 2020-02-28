package arm

import ic.org.arm.Ranges
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RangesTest {
  @Test
  fun ranges() {
    assertEquals(Byte.MIN_VALUE..Byte.MAX_VALUE, Ranges._8b.range)
  }
}
