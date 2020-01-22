import java.io.File
import java.util.Collections

/**
 * This file contains all funtions that should be sccessible by the testing suite, but are not
 * tests. Extension functions for the JUnit library are a good example.
 *
 * In particular, code here will not be included in the final JAR and is not visible from src/main.
 */

/**
 * Represents a WACC file usable by the testing suite. Such a file is likely from the wacc_examples
 * repo, and contains information about the expected outcome of the compilation (like the exit
 * code of the compiler, contained in [expectedReturn])
 */
data class WaccProgram(
  val file: File,
  val expectedReturn: Int,
  val expectedKeyWords: List<String>
)

/**
 * Constructs a [WaccProgram] frmo a normal [File]. [this] must be a file suitable for testing,
 * @see [WaccProgram].
 */
fun File.asProgram(): WaccProgram {
  val content = this.readLines()
  val isInvalidProg = this.path.contains("invalid")

  val errorCode = if (isInvalidProg) {
    val errorCodeLine = content.indexOf("# Exit:") + 1
    content[errorCodeLine].filter { it != '#' && it != ' ' }.toInt()
  } else 0

  val output = if (isInvalidProg) {
    val outputLine = content.indexOf("# Output:") + 1
    content[outputLine].filter { it != '#' }.split(' ')
  } else Collections.emptyList()

  return WaccProgram(this, errorCode, output)
}
