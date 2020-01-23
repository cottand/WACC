@file:Suppress("ConstantConditionIf")

import ic.org.CompileResult
import ic.org.WACCCompiler
import ic.org.containsAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumingThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import java.io.File

/**
 * This test class will scan the wacc_examples directory, and attempt to compile all files, one
 * by one. If the compiler crashes with  a [NotImplementedError] exception (due to a [TODO] call)
 * then that compile is ignored.
 * Otherwise, we create a test and check what the return code and compiler output was. If it as
 * expected (read from the comments of the file itself) that test passes. For any other outcome
 * (a crash, or unexpected compiler output or error code) the test fails.
 *
 * Additionally, a file that does not have the string [testingKeyword] in its path inside the
 * project will be ignored, this allowing us to select which files testing should be enabled for.
 *
 * Tests will be reported as passed if [checkSyntaxOnly] is true and and the compiler returns TODO()
 * when codes 0 or 200 are expected, meaning that we check the syntax only and do not care about
 * semantic checking.
 */
class TestPrograms {
  // Testing constants
  private val waccExamplesPath = "./wacc_examples/"
  private val testOutputKeywords = false
  private val testingKeyword = "TEST"
  private val checkSyntaxOnly = true

  private val waccFiles =
    File(waccExamplesPath).walkBottomUp()
      .filter { it.isFile && ".wacc" in it.path }
      .filter { "TEST" in it.canonicalPath } // If a
      .map { it.asProgram() }
      .onEach { println(it) }

  /**
   * Tests a single [WACCProgram] according to [checkSyntaxOnly] and [testOutputKeywords], and
   * skips the test if compilation exits with a [NotImplementedError]
   */
  private fun testProgram(program: WACCProgram) {
    val filename = program.file.absolutePath
    val res: CompileResult =
      try {
        WACCCompiler(filename).compile()
      } catch (e: Throwable) {
        // If we are only checking the syntax and we hit a NotImplemented error, that means the
        // syntactic check succeeded and returned. So we 'fake' a 0 return code to be checked
        // later against the expected result.
        if (e is NotImplementedError && checkSyntaxOnly) {
          CompileResult.success(0)
        } else {
          assumeFalse(e is NotImplementedError)
          //If we hit an unimplemented case, ignore this test. Otherwise, we must have crashed
          // for some other reason. So fail the test case.
          System.err.println("Failed to compile ${program.file.canonicalPath} with exception:")
          fail(e)
        }
      }
    res.let { result ->
      if (checkSyntaxOnly)
        assertTrue(result.exitCode in listOf(0, 200) + program.expectedReturn)
      else
        assertEquals(program.expectedReturn, result.exitCode) {
          "Bad exit code. Compile errors: \n${result.message}"
        }
      assumingThat(testOutputKeywords) {
        assertTrue(result.message.containsAll(program.expectedKeyWords))
      }
    }
  }

  /**
   * Takes every [WACCProgram] in [waccFiles] and creates a [DynamicTest] with [testProgram]
   * Every one of these [DynamicTest]s are the unit tests that show up in the report.
   */
  @TestFactory
  fun generateTestsFromFiles() = waccFiles.map {
    DynamicTest.dynamicTest(it.file.canonicalPath) { testProgram(it) }
  }.iterator()
}

