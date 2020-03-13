package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import arrow.core.Option
import arrow.core.extensions.option.align.empty
import arrow.core.invalid
import arrow.core.some
import arrow.core.valid
import ast.graph.asGraph
import ic.org.ast.Prog
import ic.org.ast.build.asAst
import ic.org.listeners.CollectingErrorListener
import ic.org.listeners.DummyListener
import ic.org.util.Parsed
import ic.org.util.areAllValid
import ic.org.util.asLines
import ic.org.util.errors
import ic.org.util.flatMap
import ic.org.util.head
import ic.org.util.ifExsistsAnd
import ic.org.util.runCommand
import ic.org.util.tail
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock
import jasmin.Main.main as jasminMain

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
  val saveToFile = !cmds.remove("-nosave")
  val cleanAfterBuild = !cmds.remove("-noclean")
  val target = if (cmds.remove("-jvm")) JVM else ARM
  if (cmds.size != 1) {
    println("Proceeding with unrecognised arguments:")
    println("  " + cmds.tail.joinToString() + '\n')
  }
  val file = cmds.head
  val progFile = File(file)
  val progFileName = progFile.name
  val progName = progFileName.replace(".wacc", "")
  val result = WACCCompiler(file).compile(target = target).also {
    println(it.msg + '\n')
  }

  // Deal with passed arguments
  result.out.ifExsistsAnd(printAssembly) {
    println("Compiled assembly:\n\n$it\n")
  }.ifExsistsAnd(saveToFile) {
    // Replace extension with .s and save compiled text
    val newFile = "$progName${target.fileExtension}"
    val assembly = File(newFile).apply { writeText(it) }
    if (target is JVM) {
      val progClass = "$progName.class"
      val mainClass = File(progClass)
      val jarFile = "$progName.jar"
      println("Packaging JAR file $jarFile... ")
      val manifest = File("manifest").apply { writeText("Main-Class: $progName\n") }
      jasminMain(arrayOf(newFile))
      "jar cfm $jarFile ${manifest.path} $progClass -C ${JVM.classpath.dropLast(1)} ${JVM.classes}".runCommand()
      if (cleanAfterBuild) listOf(manifest, mainClass, assembly).forEach { it.delete() }
    } else
      println("Saving file with compiled assembly:\n$newFile")

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

    fun success(duration: Duration, output: String) = checkSuccess(duration).copy(out = output.some())
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
      .flatMap { it.prog().asAst(input.name.replace(".wacc", "")) }
      // Check control flow by building a graph
      .flatMap { it.checkControlFlow() }
  }

  fun compile(checkOnly: Boolean = false, target: Target): CompileResult {
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
        val assembly = when (target) {
          ARM -> prog.armAsm()
          JVM -> prog.jvmAsm()
        }
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

sealed class Target
object ARM : Target()
object JVM : Target() {
  const val classpath = "lib/*"
  const val classes = "wacc/lang/Pair.class wacc/lang/Utils.class"
}

val Target.fileExtension
  get() = when (this) {
    ARM -> ".s"
    JVM -> ".j"
  }
