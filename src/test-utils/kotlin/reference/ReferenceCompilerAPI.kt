@file:Suppress("EXPERIMENTAL_API_USAGE")

package reference

import ic.org.util.createWithDirs
import ic.org.util.joinLines
import java.io.File
import java.util.stream.Stream
import kotlin.streams.toList

object ReferenceCompilerAPI {

  fun ask(prog: File, inp: String): RefAnswer {
    val cachedName = prog.absolutePath
      .replace("wacc_examples", ".test_cache")
      .replace(".wacc", ".temp")
    val cached = File(cachedName)
    val out =
      if (cached.exists())
        cached.readLines()
      else
        "ruby refCompile ${prog.path} $inp"
          .runCommand()
          .filter { it.isNotEmpty() }.toList()
          .also { cached.createWithDirs() }
          .also { cached.writeText(it.joinLines()) }
    val assembly = out.filter { it.first().isDigit() }
      .map { it.drop(2).trim() }
      .filter { it.isNotBlank() }
      .joinLines()
    val outLineBeg = out.withIndex().last { (_, str) -> "Executing..." in str }.index + 2
    val output = out.subList(outLineBeg, out.size - 3).joinLines()
    val code = out.last { "The exit code is" in it }.filter { it.isDigit() }.toInt()
    return RefAnswer(assembly, output, code)
  }

  fun run(armProg: String, filename: String, input: String): Pair<String, Int> {
    val newFile = filename.replace(".wacc", ".s")
    File(newFile).writeText(armProg)
    val actualOut = "ruby refRun $newFile $input".runCommand().joinLines()
    return actualOut to 0 // TODO not 0
  }
}

/**
 * Represents an answer from the reference compiler. If [success] is true, then [out] represents the compiled assembly,
 * or the received errors otherwise.
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
