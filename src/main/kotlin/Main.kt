package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.Validated.Invalid
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    compile(args[0])
  }

  fun compile(filename: String): CompileResult {
    val input = File(filename)
    val stream = CharStreams.fromPath(input.toPath())
    val lexer = WACCLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = WACCParser(tokens)
    val listener = ThrowingErrorListener
    parser.removeErrorListeners()
    parser.addErrorListener(listener)
    val syntacticErrors = listener.errorsSoFar
      .also { println(it) }
    val prog = parser.prog()
    val ast by lazy { prog.asAst() }
    return when {
      !syntacticErrors.isEmpty() ->
        CompileResult(false, syntacticErrors.first().code, syntacticErrors.asLines(filename))
      ast is Invalid ->
        CompileResult(false, (ast as Invalid).e.first().code, (ast as Invalid).e.asLines(filename))
      else ->
        CompileResult(true, 0, "TODO MESSAGE") // TODO  message upon success

    }
  }
}

data class CompileResult(val success: Boolean, val exitCode: Int, val message: String)

