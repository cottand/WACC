package ic.org

import kotlinx.collections.immutable.toPersistentList
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import java.util.LinkedList

/**
 * Singleton of an ErrorListener that will collect all syntactic errors as ANTLR
 * TODO lexes?
 * the program.
 */
object CollectingErrorListener : BaseErrorListener() {

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