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
    WACCCompiler(args[0]).compile()
  }
}

data class CompileResult(val success: Boolean, val exitCode: Int, val message: String) {
  companion object {
    fun success(duration: Long) = CompileResult(true, 0, "Compiled in $duration sec.")
  }
}

class WACCCompiler(val filename: String) {

  fun compile(): CompileResult {
    val input = File(filename)
    if (!(input.exists() && input.isFile)) {
      throw IllegalArgumentException("No such file $filename")
    }
    val stream = CharStreams.fromPath(input.toPath())
    val lexer = WACCLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = WACCParser(tokens)
    val listener = CollectingErrorListener()
    parser.removeErrorListeners()
    parser.addErrorListener(listener)
    val syntacticErrors = listener.errorsSoFar
    val prog = parser.prog()
    if (syntacticErrors.isNotEmpty())
      return CompileResult(false, syntacticErrors.first().code, syntacticErrors.asLines(filename))
    val ast = prog.asAst()
    return if (ast is Invalid)
      CompileResult(false, ast.e.first().code, ast.e.asLines(filename))
    else CompileResult.success(-1) // TODO measure compilation time?
  }
}



