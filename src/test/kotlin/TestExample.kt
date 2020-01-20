@file:Suppress("SimplifyBooleanWithConstants", "UnusedImport")

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestExample {
  @Test
  fun `Math is not broken`() {
    val three = 3
    assertEquals(three, 1 + 2) { exp, act -> "Expected $exp and got $act!!" }
  }
}