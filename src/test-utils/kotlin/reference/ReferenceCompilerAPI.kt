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

@Serializable
data class RefResponse(val test: String, val upload: String, val compiler_out: String)

class ReferenceCompilerAPI {

  val client = HttpClient(CIO) {
    install(JsonFeature) {
      serializer = GsonSerializer {
        disableHtmlEscaping()
        serializeNulls()
      }
    }
  }
  private val refCompilerMargin = 2
  private val url = "https://teaching.doc.ic.ac.uk/wacc_compiler/run.cgi"

  suspend fun ask2(prog: File): RefAnswer {
    val (_, _, out) = client.post<RefResponse>(url) {
      body = MultiPartContent.build {
        add("testfile", prog.readBytes(), ContentType.Application.OctetStream, "test.wacc")
        add("options[]", "-a")
      }
    }

    return if ("Errors detected" in out)
      RefAnswer(false, out)
    else
      RefAnswer(true, out.extractCode()).print()
  }

  fun ask(prog: File): RefAnswer {
    Fuel.upload(url)
      .add {
        FileDataPart(prog, "testfile", "test.wacc", contentDisposition = "form-data")
      }
      .add { InlineDataPart("-a", "options[]", contentDisposition = "form-data") }
      .responseString().print()
    TODO()
  }

  private fun String.extractCode() = lineSequence()
    // Remove lines that are not assembly code
    .filter { it[0].toInt() in 0..Int.MAX_VALUE }
    // Remove the prefix added by the reference compiler
    .joinToString(separator = "\n") { it.drop(refCompilerMargin) }
}

/**
 * Represents an answer from the reference compiler. If [success] is true, then [out] represents the compiled assembly,
 * or the received errors otherwise.
 */
data class RefAnswer(val success: Boolean, val out: String)

