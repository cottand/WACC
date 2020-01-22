import ic.org.CompileResult
import ic.org.Main
import ic.org.containsAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Assumptions.assumingThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import java.io.File
import java.util.Collections

/**
 * This test class will scan the wacc_examples directory, and attempt to compile all files, one
 * by one. If the compiler crashes with  a [NotImplementedError] exception (due to a [TODO] call)
 * then that compile is ignored.
 * Otherwise, we create a test and check what the return code and compiler output was. If it as
 * expected (read from the comments of the file itself) that test passes. For any other outcome
 * (a crash, or unexpected compiler output or error code) the test fails.
 */
object TestPrograms {
  // Testing constants
  private const val waccExamplesPath = "../wacc_examples"
  private const val testOutputKeywords = false

  private val waccFiles = File(waccExamplesPath).walkBottomUp().toList()
    .filter { it.isFile && ".wacc" in it.path }
    .map { it.asProgram() }

  private fun File.asProgram(): WaccProgram {
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

  //@Test
  //fun temp() = println(waccFiles.map { it.asProgram() })

  data class WaccProgram(
    val file: File,
    val expectedReturn: Int,
    val expectedKeyWords: List<String>
  )

  @TestFactory
  fun generateTestsFromFiles(): Collection<DynamicTest> = waccFiles.map {
    val name = it.file.canonicalPath
    DynamicTest.dynamicTest(name) {
      val res: CompileResult =
        try {
          Main.compile(it.file.absolutePath)
        } catch (e: Throwable) {
          assumeTrue(e !is NotImplementedError)
          /* If we hit an unimplemented case, ignore this test. Otherwise, we must have crashed
          for some other reason. So fail the test case */
          System.err.println("Failed to compile $name with exception:")
          fail(e)
        }
      res.let { result ->
        assertEquals(it.expectedReturn, result.exitCode) { e, a ->
          "Bad exit code. Compile errors: ${result.message}"
        }
        assumingThat(testOutputKeywords) {
          assertTrue(result.message.containsAll(it.expectedKeyWords))
        }
      }
    }
  }
}

