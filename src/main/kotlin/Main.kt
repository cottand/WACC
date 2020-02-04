package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.asAst
import ic.org.grammar.Prog
import ic.org.graph.asGraph
import ic.org.listeners.CollectingErrorListener
import ic.org.listeners.DummyListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

@ExperimentalTime
fun main(args: Array<String>) {
  val (_, exitCode, msg) = WACCCompiler(args[0]).compile()
  println(msg)
  exitProcess(exitCode)
}

@ExperimentalTime
data class CompileResult(val success: Boolean, val exitCode: Int, val message: String) {
  companion object {
    fun success(duration: Duration) = CompileResult(
      success = true,
      exitCode = 0,
      message = "Compiled in ${duration.inMilliseconds.toLong()}ms"
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
    return checkSyntax(parser)
      .flatMap { it.prog().asAst() }
      .flatMap { it.checkControlFlow() }
      .fold({ errors ->
        CompileResult(
          success = false,
          exitCode = errors.first().code,
          message = errors.asLines(filename)
        )
      }, {
        CompileResult.success(duration = start.elapsedNow())
      })
  }

  companion object {
    private fun checkSyntax(parser: WACCParser): Parsed<WACCParser> {
      val listener = CollectingErrorListener()
      parser.removeErrorListeners()
      parser.addErrorListener(listener)
      parser.tokenStream.seek(0)
      ParseTreeWalker().walk(DummyListener(), parser.prog())
      parser.tokenStream.seek(0)
      return if (listener.errorsSoFar.isEmpty())
        parser.valid()
      else
        listener.errorsSoFar.invalid()
    }

    fun Prog.checkControlFlow(): Parsed<Prog> = funcs
      .map { it.stat.asGraph(it.retType) to it }
      .map { (graph, func) -> graph.checkReturnType(func.retType, func.ident) }
      .let {
        if (it.areAllValid)
          this.valid()
        else
          it.errors.invalid()
      }
  }
}
