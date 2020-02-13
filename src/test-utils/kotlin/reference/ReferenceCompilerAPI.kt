@file:Suppress("EXPERIMENTAL_API_USAGE")

package reference

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.InlineDataPart
import ic.org.util.print
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.post
import io.ktor.http.ContentType
import kotlinx.serialization.Serializable
import java.io.File
import java.util.stream.Stream
import kotlin.streams.toList

@Serializable
data class RefResponse(val test: String, val upload: String, val compiler_out: String)

object ReferenceCompilerAPI {

  fun ask(prog: File): RefAnswer =
    "ruby refCompile -a ${prog.path}".runCommand()
      .filter { it.isNotEmpty() }
      .filter { it[0] in '0'..'9' }
      .map { it.drop(2).trim() }
      .filter { it.isNotBlank() }
      .toList()
      // Remove the prefix added by the reference compiler
      .joinToString(separator = "\n").trim()
      .let { RefAnswer(true, it) }

  private fun String.runCommand(workingDir: File = File(".")): Stream<String> =
    ProcessBuilder(*split("\\s".toRegex()).toTypedArray())
      .directory(workingDir)
      // .redirectOutput(ProcessBuilder.Redirect.PIPE)
      // .redirectError(ProcessBuilder.Redirect.PIPE)
      .start()
      .inputStream
      .reader()
      .buffered()
      .lines()
}

/**
 * Represents an answer from the reference compiler. If [success] is true, then [out] represents the compiled assembly,
 * or the received errors otherwise.
 */
data class RefAnswer(val success: Boolean, val out: String)

