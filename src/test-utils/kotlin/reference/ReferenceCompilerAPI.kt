@file:Suppress("EXPERIMENTAL_API_USAGE")

package reference

import arrow.core.getOrElse
import arrow.core.toOption
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.InlineDataPart
import com.github.kittinunf.fuel.gson.responseObject
import com.google.gson.Gson
import ic.org.util.NOT_REACHED
import ic.org.util.createWithDirs
import ic.org.util.joinLines
import ic.org.util.print
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

object ReferenceCompilerAPI {

  private val gson by lazy { Gson() }
  private const val noCacheToken = "NO_CACHE"

  private const val delimiters = "==========================================================="

  fun ask(prog: File, input: String): RefAnswer {
    val emulatorUrl = "https://teaching.doc.ic.ac.uk/wacc_compiler/run.cgi"
    val cachedName = prog.absolutePath
      .replace("wacc_examples", ".test_cache")
      .replace(".wacc", ".temp")
    val cached = File(cachedName)
    val out =
      if (noCacheToken !in prog.path && cached.exists() && cached.readText().isNotBlank())
        cached.readLines()
          .also { println("Reading cached query...") }
      else
        queryReference<CompilerReply>(prog, emulatorUrl, input, "-x", "-a")
          .getOrElse { System.err.println("Failed to reach the reference emulator"); assumeTrue(false); NOT_REACHED() }
          .compiler_out.split("\n")
          .toList()
          .also { cached.createWithDirs() }
          .also { cached.writeText(it.joinLines()) }
    val assembly = out
      .asSequence()
      .dropWhile { delimiters !in it }
      .drop(1)
      .takeWhile { delimiters !in it }
      .map { str -> str.dropWhile { chr -> chr.isDigit() }.trim() }
      .filter { it.isNotBlank() }
      .toList()
      .joinLines()
    val runtimeOut = out
      .dropWhile { "-- Executing..." !in it }
      .drop(2)
      .takeWhile { "The exit code is" !in it }
      .map { if (delimiters in it) it.replace(delimiters, "") else it }
      .joinLines()
    val code = try {
      out.last { "The exit code is" in it }.filter { str -> str.isDigit() }.toInt()
    } catch (e: Exception) {
      System.err.println("Could not parse exit code from reference compiler. Output:\n${out.joinLines()}\n")
      -1
    }
    return RefAnswer(assembly, runtimeOut, code)
  }

  fun emulate(armProg: String, filename: String, input: String): Pair<String, Int> {
    val emulatorUrl = "https://teaching.doc.ic.ac.uk/wacc_compiler/emulate.cgi"
    val newFile = filename.replace(".wacc", ".s")
    val armFile = File(newFile).apply { writeText(armProg) }
    val reply = queryReference<EmulatorReply>(armFile, emulatorUrl, stdin = input)
      .getOrElse { System.err.println("Failed to reach the reference emulator"); assumeTrue(false); NOT_REACHED() }
    if (reply.assemble_out.isNotBlank()) {
      val asmWithLines = armProg.lines().mapIndexed { i, s ->
        val lNo = (i + 1).toString()
        lNo + "      ".drop(lNo.length) + s
      }.joinLines()
      throw IllegalStateException("Failed to assemble program with error:\n${reply.assemble_out}\n$asmWithLines")
    }
    return reply.emulator_out to reply.emulator_exit.toInt()
  }

  private inline fun <reified T : Any> queryReference(f: File, url: String, stdin: String = "", vararg ops: String) =
    ops.asSequence()
      .map { InlineDataPart(it, "options[]") }
      .toList().toTypedArray()
      .let { options ->
        Fuel.upload(url)
          .add(FileDataPart(f, "testfile", f.name, "application/octet-stream"))
          .add(*options)
          .apply { if (stdin.isNotBlank()) add(InlineDataPart(stdin, "stdin")) }
          .responseObject<T>(gson).third
          .component1()
          .toOption()
      }
}

/**
 * Represents an answer from the reference compiler.[out] represents the compiled assembly,
 * or the received errors.
 */
data class RefAnswer(val assembly: String, val out: String, val code: Int)

/**
 * Serialised JSON produced by the reference emulator
 */
data class EmulatorReply(
  val test: String,
  val upload: String,
  val assemble_out: String,
  val emulator_out: String,
  val emulator_exit: String
)

/**
 * Serialised JSON produced by the reference compiler
 */
data class CompilerReply(val test: String, val upload: String, val compiler_out: String)


