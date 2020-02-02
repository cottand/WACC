@file:Suppress("ConstantConditionIf")

import ic.org.CompileResult
import ic.org.WACCCompiler
import ic.org.containsAll
import ic.org.print
import java.io.File
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumingThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail

/**
 * This test class will scan the wacc_examples directory, and attempt to compile all files, one
 * by one. If the compiler crashes with  a [NotImplementedError] exception (due to a [TODO] call)
 * then that compile is ignored.
 * Otherwise, we create a test and check what the return code and compiler output was. If it as
 * expected (read from the comments of the file itself) that test passes. For any other outcome
 * (a crash, or unexpected compiler output or error code) the test fails.
 *
 * Additionally, a file that does not have the string [testingKeyword] or [ignoreKeyword] in its
 * path inside the project will be ignored, this allowing us to select which files testing should
 * be enabled for.
 *
 * [ignoreKeyword] takes precedence. Ie, if a directory has [testingKeyword] but a subdirectory has
 * [ignoreKeyword], the directory will be tested except for the subdirectory/
 *
 */
@ExperimentalTime
class TestPrograms {
  // Testing constants
  private val waccExamplesPath = "./wacc_examples/"
  private val testOutputKeywords = false
  private val testingKeyword = "TEST"
  private val ignoreKeyword = "IGNORE"
  private val testSemanticsOnly = true

  private val waccFiles =
    File(waccExamplesPath).walkBottomUp()
      .filter { it.isFile && ".wacc" in it.path }
      .filter { "TEST" in it.canonicalPath } // If a
      .filterNot { "IGNORE" in it.canonicalPath }
      .map { it.asProgram() }
      .toList()

  /**
   * Tests whether the [WACCCompiler] can perform all syntactic checks on a [program] with the
   * expected output
   */
  private fun testSyntax(program: WACCProgram) {
    program.file.readText().print()
    program.file.canonicalPath.print()
    val filename = program.file.absolutePath
    println(filename)
    val res: CompileResult =
      try {
        WACCCompiler(filename).compile()
      } catch (e: Throwable) {
        // If we are only checking the syntax and we hit a NotImplemented error, that means the
        // syntactic check succeeded and returned. So we 'fake' a 0 return code to be checked
        // later against the expected result.
        if (e is NotImplementedError) {
          // TODO Check the Duration casting
          CompileResult.success(Duration.ZERO)
        } else {
          // If we hit an unimplemented case, ignore this test. Otherwise, we must have crashed
          // for some other reason. So fail the test case.
          System.err.println("Failed to compile ${program.file.canonicalPath} with exception:")
          fail(e)
        }
      }
    if (program.expectedReturn == 100)
      assertEquals(100, res.exitCode)
    else
      assertTrue(res.exitCode in listOf(0, 200)) {
        "Unexpected failure, expected a succesful syntax check (and that did not happen).\n" +
          "  Errors:\n ${res.message}"
      }
  }

  /**
   * Tests whether [WACCCompiler] can compile  [program] with the expected compile output (so no
   * runtime execution tests) according to [testOutputKeywords]
   */
  @ExperimentalTime
  private fun testSemantics(program: WACCProgram) {
    val filename = program.file.absolutePath
    val res: CompileResult =
      try {
        WACCCompiler(filename).compile()
      } catch (e: Throwable) {
        assumeFalse(e is NotImplementedError)
        // If we hit an unimplemented case, ignore this test. Otherwise, we must have crashed
        // for some other reason. So fail the test case.
        System.err.println("Failed to compile ${program.file.canonicalPath} with exception:")
        fail(e)
      }
    res.let { result ->
      assertEquals(program.expectedReturn, result.exitCode) {
        "Bad exit code. Compile errors: \n${result.message}"
      }
      assumingThat(testOutputKeywords) {
        assertTrue(result.message.containsAll(program.expectedKeyWords))
      }
    }
  }

  /**
   * Takes every [WACCProgram] in [waccFiles] and creates a [DynamicTest] with [testSemantics]
   * Every one of these [DynamicTest]s are the unit tests that show up in the report.
   */
  @ExperimentalTime
  @TestFactory
  fun semanticallyCheckPrograms() = waccFiles.map {
    DynamicTest.dynamicTest(it.file.canonicalPath) { testSemantics(it) }
  }

  /**
   * Takes every [WACCProgram] in [waccFiles] and creates a [DynamicTest] with [testSyntax]
   * Every one of these [DynamicTest]s are the unit tests that show up in the report.
   */
  @ExperimentalTime
  @TestFactory
  fun syntacticallyCheckPrograms() = waccFiles
    .filterNot { testSemanticsOnly }
    .map {
    DynamicTest.dynamicTest(it.file.canonicalPath) { testSyntax(it) }
  }
}
