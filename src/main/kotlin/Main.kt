package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import ic.org.grammar.GlobalScope
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

@ExperimentalTime
object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    WACCCompiler(args[0]).compile()
  }
}

@ExperimentalTime
data class CompileResult(val success: Boolean, val exitCode: Int, val message: String) {
  companion object {
    fun success(duration: Duration) = CompileResult(
      success = true,
      exitCode = 0,
      message = "Compiled in ${duration.inSeconds}.${duration.inMilliseconds % 1000}"
    )
  }
}

@ExperimentalTime
class WACCCompiler(private val filename: String) {
  fun compile(): CompileResult {
    val start = MonoClock.markNow()
    val input = File(filename)
    if (!(input.exists() && input.isFile)) {
      throw IllegalArgumentException("No such file $filename")
    }
    val stream = CharStreams.fromPath(input.toPath())
    val lexer = WACCLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = WACCParser(tokens)
    val syntacticErrors = checkSyntax(parser)
    return if (syntacticErrors.isNotEmpty())
      CompileResult(
        success = false,
        exitCode = syntacticErrors.first().code,
        message = syntacticErrors.asLines(filename)
      )
    else when (val ast = parser.prog().asAst(GlobalScope)) {
      is Valid -> CompileResult.success(duration = start.elapsedNow())
      is Invalid -> CompileResult(
        success = false,
        exitCode = ast.e.first().code,
        message = ast.e.asLines(filename)
      )
    }
  }

  companion object {
    private fun checkSyntax(parser: WACCParser): List<SyntacticError> {
      val listener = CollectingErrorListener()
      parser.removeErrorListeners()
      parser.addErrorListener(listener)
      parser.tokenStream.seek(0)
      ParseTreeWalker().walk(DummyListener(), parser.prog())
      parser.tokenStream.seek(0)
      return listener.errorsSoFar
    }
  }
}



