package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.extensions.validated.foldable.fold
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
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
    val prog = parser.prog().asAst()
    return when(prog) {
      is Valid -> CompileResult(true, 0, "TODO")
      is Invalid -> {
        val first = prog.e.first()
        CompileResult(false, first.code, first.msg)
      }
    }

  }
}

data class CompileResult(val success: Boolean, val exitCode: Int, val message: String)

