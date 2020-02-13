package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.Option
import arrow.core.extensions.option.align.empty
import arrow.core.invalid
import arrow.core.some
import arrow.core.valid
import arrow.syntax.collections.tail
import ast.graph.asGraph
import ic.org.ast.Prog
import ic.org.ast.build.asAst
import ic.org.instr.instr
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
  println()
  if (args.size !in 1..6) {
    println("Unexpected number of arguments: ${args.size}")
    println("Expected at least the file to be compiled.")
    exitProcess(-1)
  }

  val cmds = args.toMutableList()
  val printAssembly = cmds.remove("-a")
  val saveToFile = cmds.remove("-save")
  if (cmds.size != 1) {
    println("Proceeding with unrecognised arguments:")
    println("  " + cmds.tail.joinToString() + '\n')
  }
  val file = cmds.head
  val result = WACCCompiler(file).compile()
  println(result.msg + '\n')

  // Deal with passed arguments

  result.out.ifExsistsAnd(printAssembly) {
    println("Compiled assembly:\n\n$it\n")
  }.ifExsistsAnd(saveToFile) {
    // Replace extension with .s and save compiled text
    val newFile = file.dropLast(".wacc".length) + ".s"
    println("Saving file with compiled assembly:\n$newFile")
    File(newFile).writeText(it)
  }
  exitProcess(result.exitCode)
}

@ExperimentalTime
data class CompileResult(val success: Boolean, val exitCode: Int, val msg: String, val out: Option<String> = empty()) {
  companion object {
    fun checkSuccess(duration: Duration) = CompileResult(
      success = true,
      exitCode = 0,
      msg = "Compiled in ${duration.inMilliseconds.toLong()}ms"
    )

    fun success(duration: Duration, output: String) = CompileResult(
      success = true,
      exitCode = 0,
      msg = "Compiled in ${duration.inMilliseconds.toLong()}ms",
      out = output.some()
    )
  }
}

@ExperimentalTime
class WACCCompiler(private val filename: String) {

  /**
   * Compiler front-end entrypoint
   */
  private fun check(): Parsed<Prog> {
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
  }

  private fun Prog.toAssembly(): String = instr().joinToString(separator = "\n") { it.code }

  fun compile(checkOnly: Boolean = false): CompileResult {
    val start = MonoClock.markNow()
    // Make a CompileResultout of the outcome
    return check().fold({ errors ->
      // If the checks found a malformed program:
      CompileResult(
        success = false,
        exitCode = errors.first().code,
        msg = errors.asLines(filename)
      )
    }, { prog ->
      // If checks were succesful, compile depending on checkOnly
      if (checkOnly)
        CompileResult.checkSuccess(duration = start.elapsedNow())
      else {
        val assembly = prog.toAssembly()
        CompileResult.success(duration = start.elapsedNow(), output = assembly)
      }
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
    private fun Prog.checkControlFlow(): Parsed<Prog> = funcs
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
