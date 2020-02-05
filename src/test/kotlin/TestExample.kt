@file:Suppress("SimplifyBooleanWithConstants", "UnusedImport")

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestExample {
  @Test
  fun `Math is not broken`() {
    val three = 3
    assertEquals(three, 1 + 2) { "Expected ${1 + 2} and got $three!" }
  }
}
