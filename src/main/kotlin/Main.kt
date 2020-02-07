package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.Prog
import ic.org.ast.build.asAst
import ic.org.graph.asGraph
import ic.org.listeners.CollectingErrorListener
import ic.org.listeners.DummyListener
import ic.org.util.*
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
  if (args.size != 1) {
    println(
      "Unexpected number of arguments: ${args.size}\n" +
        "Expected a single argument, the file to be compiled."
    )
    exitProcess(-1)
  }
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
    // Check syntax
    return parser.checkSyntax()
      // Check semantics by building the AST
      .flatMap { it.prog().asAst() }
      // Check control flow by building a graph
      .flatMap { it.checkControlFlow() }
      // Make a CompileResultout of the outcome
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
    /**
     * Checks the program's syntax by making a [DummyListener] walk the [WACCParser], thus making antlr accumulate all the
     * errors it may find into a [CollectingErrorListener].
     * @return the errors accumulated by the [CollectingErrorListener], or the [WACCParser], boxed into a [Parsed]
     */
    private fun WACCParser.checkSyntax(): Parsed<WACCParser> {
      val listener = CollectingErrorListener()
      removeErrorListeners()
      addErrorListener(listener)
      tokenStream.seek(0)
      ParseTreeWalker().walk(DummyListener(), prog())
      tokenStream.seek(0)
      return if (listener.errorsSoFar.isEmpty())
        valid()
      else
        listener.errorsSoFar.invalid()
    }

    /**
     * Makes an additional pass to the AST in order to check correctness of the control flow.
     */
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
