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

typealias Errors = PersistentList<CompilationError>
typealias Parsed<A> = Validated<Errors, A>

sealed class CompilationError {
  abstract val msg: String
  abstract val code: Int
}

class SyntacticError(override val msg: String) : CompilationError() {
  override val code = 100
}

class ThrowingErrorListener : BaseErrorListener() {
  @Throws(ParseCancellationException::class)
  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
    throw ParseCancellationException("line $line:$charPositionInLine $msg")
  }

  companion object {
    val INSTANCE = ThrowingErrorListener()
  }
}

sealed class SemanticError : CompilationError() {
  override val code = 200
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
