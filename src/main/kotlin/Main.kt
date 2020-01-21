package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    ic.org.Entry.compile(args[0])
  }
}

object Entry {
  public fun compile(filename: String) {
    println(this::javaClass)

    val input = File(filename)
    val stream = CharStreams.fromPath(input.toPath())
    val lexer = WACCLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = WACCParser(tokens)
    val prog = parser.prog()
    println(prog.func()[0].ID().toString())
  }
}