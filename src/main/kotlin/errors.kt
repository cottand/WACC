package ic.org

import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.extensions.list.foldable.forAll
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.lang.IllegalStateException
import java.util.LinkedList

typealias Errors = PersistentList<CompilationError>
typealias Parsed<A> = Validated<Errors, A>

sealed class CompilationError {
  abstract val msg: String
  abstract val code: Int
}
data class SyntacticError(override val msg: String) : CompilationError() {
  override val code = 100
}

sealed class SemanticError : CompilationError() {
  override val code = 200
}

/**
 * Singleton of an ErrorListener that will collect all syntactic errors as ANTLR
 * TODO lexes?
 * the program.
 */
object CollectingErrorListener : BaseErrorListener() {

  private val errors = LinkedList<SyntacticError>()

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


inline val <reified A> Parsed<A>.errors: Errors
  get() = when (this) {
    is Invalid -> this.e
    else -> persistentListOf()
  }

inline val <reified A> List<Parsed<A>>.errors: Errors
  get() = this.flatMap { it.errors }.toPersistentList()

inline val <reified A> List<Parsed<A>>.allValid: Boolean
  get() = this.forAll { it is Valid }

/**
 * Formats a [List] of [CompilationError]s into pretty a [String] ready to be used as a summary of
 * all the errors in [this]
 */
fun List<CompilationError>.asLines(filename: String) =
  "In file $filename:\n" +
    fold("") { str, err -> "$str  ${err.msg}\n" } + '\n'