@file:Suppress("EXPERIMENTAL_API_USAGE")

package reference

import ic.org.util.joinLines
import java.io.File
import java.util.stream.Stream
import kotlin.streams.toList

object ReferenceCompilerAPI {

  fun ask(prog: File, inp: String): RefAnswer {
    val out = "ruby refCompile ${prog.path} $inp"
      .runCommand()
      .filter { it.isNotEmpty() }.toList()
    val assembly = out.filter { it.first().isDigit() }
      .map { it.drop(2).trim() }
      .filter { it.isNotBlank() }
      .joinLines()
    val outLineBeg = out.withIndex().last { (_, str) -> "Executing..." in str }.index + 2
    val outLineEnd = out.size - 3
    val output = out.subList(outLineBeg, outLineEnd).joinLines()
    val code = out.last { "The exit code is" in it }.filter { it.isDigit() }.toInt()
    return RefAnswer(assembly, output, code)
  }

  fun run(armProg: String, filename: String, input: String) : Pair<String, Int> {
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
