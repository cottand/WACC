package ic.org

import antlr.WACCLexer
import antlr.WACCParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.File

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    ic.org.Entry.compile(args[0])
  }
}

object Entry {
  fun compile(filename: String) {
    val input = File(filename)
    val stream = CharStreams.fromPath(input.toPath())
    val lexer = WACCLexer(stream)
    val tokens = CommonTokenStream(lexer)
    val parser = WACCParser(tokens)
    val prog = parser.prog()
    val walker = ParseTreeWalker()
    println(prog.stat().toInfoString(parser))
  }
}