import org.junit.jupiter.api.Assertions

fun <A, B> assertEquals(expected: A, actual: B, lazyMessage: (A, B) -> String) {
  val lazy = { lazyMessage(expected, actual) }
  return Assertions.assertEquals(expected, actual, lazy)
}