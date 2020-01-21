package ic.org

import antlr.WACCParser
import arrow.core.Either


class  AST {
}

fun WACCParser.asAST() : Either<CompilationError, AST> {
  TODO()
}
