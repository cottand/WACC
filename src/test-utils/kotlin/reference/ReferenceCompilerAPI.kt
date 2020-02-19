@file:Suppress("EXPERIMENTAL_API_USAGE")

package reference

import arrow.core.getOrElse
import arrow.core.lastOrNone
import com.google.gson.Gson
import ic.org.util.createWithDirs
import ic.org.util.joinLines
import java.io.File
import java.lang.IllegalStateException
import java.util.stream.Stream
import kotlin.streams.toList

object ReferenceCompilerAPI {

  private val gson by lazy { Gson() }

  private inline fun <reified T> String.fromJson() = gson.fromJson(this, T::class.java)

  private const val delimiters = "==========================================================="

  fun ask(prog: File, inp: String): RefAnswer {
    val cachedName = prog.absolutePath
      .replace("wacc_examples", ".test_cache")
      .replace(".wacc", ".temp")
    val cached = File(cachedName)
    val out =
      if (cached.exists() && cached.readText().isNotBlank())
        cached.readLines()
          .also { println("Reading cached ruby query...") }
      else
        "ruby refCompile ${prog.path} $inp"
          .runCommand()
          .filter { it.isNotEmpty() }.toList()
          .also { cached.createWithDirs() }
          .also { cached.writeText(it.joinLines()) }
    val assembly = out
      .asSequence()
      .dropWhile { delimiters !in it }
      .drop(1)
      .takeWhile { delimiters !in it }
      .map { it.drop(2).trim() }
      .filter { it.isNotBlank() }
      .toList()
      .joinLines()
    val runtimeOut = out
      .dropWhile { "-- Executing..." !in it }
      .drop(2)
      .takeWhile { "The exit code is" !in it }
      .map { if (delimiters in it) it.replace(delimiters, "") else it }
      .joinLines()
    val code = out.lastOrNone() { "The exit code is" in it }
      .getOrElse { throw IllegalStateException("Failed to parse:\n${out.joinLines()}") }
      .filter { str -> str.isDigit() }.toInt()
    return RefAnswer(assembly, runtimeOut, code)
  }

  fun run(armProg: String, filename: String, input: String): Pair<String, Int> {
    val newFile = filename.replace(".wacc", ".s")
    File(newFile).writeText(armProg)
    val reply = "ruby refRun $newFile $input".runCommand().joinLines().fromJson<EmulatorReply>()
    return reply.emulator_out to reply.emulator_exit.toInt()
  }
}

/**
 * Represents an answer from the reference compiler.[out] represents the compiled assembly,
 * or the received errors.
 */
data class RefAnswer(val assembly: String, val out: String, val code: Int)

@Suppress("BlockingMethodInNonBlockingContext")
fun String.runCommand(workingDir: File = File(".")): Stream<String> =
  ProcessBuilder(*split("\\s".toRegex()).toTypedArray())
    .directory(workingDir)
    .start()
    .apply { waitFor() }
    .inputStream.bufferedReader()
    .lines()

data class EmulatorReply(
  val test: String,
  val upload: String,
  val assemble_out: String,
  val emulator_out: String,
  val emulator_exit: String
)

