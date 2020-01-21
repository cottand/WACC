package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.Either
import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.extensions.validated.foldable.fold
import arrow.core.left
import arrow.core.right
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
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
    parser.removeErrorListeners()
    parser.addErrorListener(ThrowingErrorListener.INSTANCE)
    val prog = runBlocking {
      Either.Companion.catch( //TODO collect without grabbing a throwable????
        { persistentListOf(SyntacticError(it.localizedMessage)) },
        { parser.prog() })
    }
    val valid: Parsed<WACCParser.ProgContext> = Validated.fromEither(prog) // AST
    return when (valid) {
      is Valid -> CompileResult(true, 0, "TODO")
      is Invalid -> {
        val msg = valid.e.toString() // TODO format this!
        val code = valid.e.first().code
        CompileResult(false, code, msg)
      }
    }
  }
}

data class CompileResult(val success: Boolean, val exitCode: Int, val message: String)

