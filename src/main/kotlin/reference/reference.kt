package ic.org.reference

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import java.io.File

class ReferenceCompilerAPI {
  private val client by lazy { HttpClient() }
  private val opts = listOf("-a")
  private val refCompilerMargin = 2

  suspend fun askReference(prog: File): RefAnswer {
    val resp = client.post<RefResponse>("https://teaching.doc.ic.ac.uk/wacc_compiler/run.cgi") {
      body = object {
        val testfile = prog.readBytes();
        val options = opts
        // val stdin = ""
        // val run = ""
      }
    }
    val out = resp.compiler_out
    return if ("Errors detected" in out)
      RefAnswer(false, out)
    else {
      val txt = out.lines()
        // Remove lines that are not assembly code
        .filter { it[0].toInt() in 0..Int.MAX_VALUE }
        // Remove the prefix added by the reference compiler
        .joinToString(separator = "\n") { it.drop(2) }
      RefAnswer(true, txt)
    }
  }
}

data class RefResponse(val test: String, val upload: String, val compiler_out: String)

/**
 * Represents an answer from the reference compiler. If [success] is true, then [out] represents the compiled assembly,
 * or the received errors otherwise.
 */
data class RefAnswer(val success: Boolean, val out: String)
