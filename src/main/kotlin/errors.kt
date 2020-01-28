package ic.org

import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.extensions.list.foldable.forAll
import arrow.core.invalid
import arrow.core.valid
import ic.org.grammar.Type
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Useful extension functions and sealed classes to deal with errors at compile time.
 * @see [CompilationError]
 */
typealias Errors = PersistentList<CompilationError>

typealias Parsed<A> = Validated<Errors, A>

private const val syntacticErrorCode = 100
private const val semanticErrorCode = 200

/**
 * Represents an error at compile time. Produced by either the [CollectingErrorListener] as a
 * [SyntacticError], or by [Prog.asAst()] or one of its recursive calls as a [SemanticError].
 *
 * - [msg]: the message that will be printed for the user when reporting the error
 *
 * - [code]: the exit code (the compiler's) that this error would cause. One of
 *   [syntacticErrorCode] or [semanticErrorCode].
 */
sealed class CompilationError {
  abstract val msg: String
  abstract val code: Int
  fun toInvalidParsed() = persistentListOf(this).invalid()
}

data class SyntacticError(override val msg: String) : CompilationError() {
  override val code = syntacticErrorCode
}

sealed class SemanticError : CompilationError() {
  override val code = semanticErrorCode
}

data class VarNotFoundError(override val msg: String) : SemanticError() {
  constructor(pos: Position, varName: String) : this("$pos, variable not defined: $varName")
}

data class TypeError(override val msg: String) : SemanticError() {
  constructor(pos: Position, expectedTs: List<Type>, actual: Type, op: String)
    : this("$pos, for operation `$op`, expected some type ${expectedTs}, actual: $actual")

  constructor(pos: Position, expectedT: Type, actual: Type, op: String)
    : this("$pos, for operation `$op`, expected type ${expectedT}, actual: $actual")
}

data class ControlFlowTypeError(override val msg: String) : SemanticError() {
  constructor(thenType: Type, elseType: Type) :
    this(
      "differing returning types in if-else branching.\n" +
        "    Got `$thenType` in the 'then' branch and `$elseType` in the 'else' branch"
    )
  constructor(type: Type)
  : this("missing return statement on branch. Expecting `$type`")
  override fun toString() = msg
  fun asTypeError(pos: Position) = TypeError("$pos, $this")
}

data class UndefinedOp(override val msg: String) : SemanticError() {
  constructor(pos: Position, op: String, vararg ts: Type)
    : this("$pos, undefined operation `$op` for types $ts")
}

data class IllegalArrayAccess(override val msg: String) : SemanticError() {
  constructor(pos: Position, expr: String, badT: Type)
    : this("$pos, illegal type in `$expr` for array acces. Expected an Int, actual: $badT")
}

inline val <A> Parsed<A>.errors: Errors
  get() = when (this) {
    is Invalid -> this.e
    else -> persistentListOf()
  }

inline val <A> List<Parsed<A>>.errors: Errors
  get() = this.flatMap { it.errors }.toPersistentList()

inline val <A> List<Parsed<A>>.areAllValid: Boolean
  get() = this.forAll { it is Valid }

/**
 * Formats a [List] of [CompilationError]s into pretty a [String] ready to be used as a summary of
 * all the errors in [this]
 */
fun List<CompilationError>.asLines(filename: String) =
  "In file $filename:\n" +
    fold("") { str, err -> "$str  ${err.msg}\n" } + '\n'

fun <A, B> Parsed<A>.flatMap(transform: (A) -> Parsed<B>): Parsed<B> = when (this) {
  is Valid -> transform(a)
  is Invalid -> this
}

data class Position(val l: Int, val col: Int) {
  override fun toString(): String = "At $l:$col"
}