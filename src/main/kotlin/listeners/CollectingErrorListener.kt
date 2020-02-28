package ic.org.listeners

import ic.org.util.SyntacticError
import java.util.LinkedList
import kotlinx.collections.immutable.toPersistentList
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

/**
 * An ErrorListener that will collect all syntactic errors as ANTLR
 * parses the program.
 */
class CollectingErrorListener : BaseErrorListener() {

  private val errors =
    LinkedList<SyntacticError>()

  val errorsSoFar
    get() = errors.toPersistentList()

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
    val error = SyntacticError("At $line:$charPositionInLine, $msg")
    errors.push(error)
  }
}
