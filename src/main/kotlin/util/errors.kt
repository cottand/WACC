@file:Suppress("LeakingThis")

package ic.org.util

import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.extensions.list.foldable.forAll
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
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
 * Represents an error at compile time. Produced by either the CollectingErrorListener as a
 * [SyntacticError], or by [Prog.asAst()] or one of its recursive calls as a [SemanticError].
 *
 * - [msg]: the message that will be printed for the user when reporting the error
 *
 * - [code]: the exit code (the compiler's) that this error would cause. One of
 *   [syntacticErrorCode] or [semanticErrorCode].
 */
sealed class CompilationError {
  private val type = if (this is SyntacticError) "Syntactic" else "Semantic"
  val message = "$type error, $msg"
  internal abstract val msg: String
  abstract val code: Int
  fun toInvalidParsed() = persistentListOf(this).invalid()
}

open class SyntacticError(override val msg: String) : CompilationError() {
  override val code = syntacticErrorCode
}

sealed class SemanticError : CompilationError() {
  override val code = semanticErrorCode
}

data class UndefinedIdentifier(override val msg: String) : SemanticError() {
  constructor(pos: Position, id: String) : this("$pos, undefined identifier: $id")
}

data class TypeError(override val msg: String) : SemanticError() {
  constructor(pos: Position, expectedTs: List<Type>, actual: Type, op: String) :
    this("$pos, for operation `$op`, expected some type $expectedTs, actual: $actual")

  constructor(pos: Position, expectedTs: List<Type>, actualTs: Pair<Type, Type>, op: String, expr: Expr) :
    this(
      "$pos, for operation `$op`, " +
        "expected two of type(s) $expectedTs, actuals: ${actualTs.first} and ${actualTs.second}\n" +
        "    in expression: $expr"
    )


  constructor(pos: Position, expectedTs: List<Type>, actual: Type, op: String, expr: Expr) :
    this("$pos, for operation `$op`, expected some type $expectedTs, actual: $actual\n" +
    "    in expression: $expr")

  constructor(pos: Position, expectedT: Type, actual: Type, op: String) :
    this("$pos, for operation `$op`, expected type $expectedT, actual: $actual")

  constructor(pos: Position, expectedT: Type, actual: Type, op: String, rhs: AssRHS) :
    this("$pos, for operation `$op`, expected type $expectedT, actual: $actual\n" +
      "    in: $rhs")

  constructor(pos: Position, expectedT: Type, op: String, expr: Expr) :
    this("$pos, for operation `$op`, expected type $expectedT, actual: ${expr.type}\n" +
      "    in expression:$expr")
}

data class NullPairError(val pos: Position) : SemanticError() {
  override val msg = "$pos, tried to reference expression of type Pair that resolves to `null`"
}

data class ControlFlowTypeError(override val msg: String) : SyntacticError(msg) {
  constructor(pos: Position, thenType: Type, elseType: Type) :
    this(
      "$pos, differing returning types in if-else branching.\n" +
        "    Got `$thenType` in the 'then' branch and `$elseType` in the 'else' branch"
    )

  constructor(pos: Position, stat: String) :
    this("$pos, unreachable statement `$stat`")

  constructor(pos: Position, type: Type) :
    this("$pos, missing return statement on branch. Expecting `$type`")

  override fun toString() = msg
}

data class IllegalArrayAccess(override val msg: String) : SemanticError() {
  constructor(pos: Position, expr: String, badT: Type) :
    this("$pos, illegal type in `$expr` for array acces. Expected an Int, actual: $badT")
}

data class InvalidReturn(override val msg: String) : SemanticError() {
  constructor(pos: Position) :
    this("$pos, `return` statement is not allowed in given scope (use `exit` maybe?)")
}

data class IntegerOverflowError(override val msg: String) : SyntacticError(msg) {
  constructor(pos: Position, i: Number) :
    this("$pos, invalid integer `$i`. Not in ${IntLit.range}")
}

data class IllegalFunctionReturnTypeError(override val msg: String) : SemanticError() {
  constructor(pos: Position, ident: Ident, expected: Type, actual: Type) :
    this("$pos, `${ident.name}`, expected return type `$expected` but found `$actual`")
}

data class RedeclarationError(val pos: Position, val ident: Ident) : SemanticError() {
  override val msg: String = "$pos, already delcared in scope: `${ident.name}`"
}

data class DuplicateParamError(val pos: Position, val ident: Ident) : SemanticError() {
  override val msg: String = "$pos, duplicate param found: `${ident.name}`"
}

data class UnexpectedNumberOfParamsError(val pos: Position, val ident: Ident, val expected: Int, val actual: Int) :
  SemanticError() {
  override val msg: String =
    "$pos, unexpected number of parameters in funciton call: `${ident.name}`, expected: $expected, actual: $actual"
}

/**
 * A [Parsed]'s [Errors] if it is [Invalid], or an empty [PersistentList] otherwise.
 */
inline val <A> Parsed<A>.errors: Errors
  get() = when (this) {
    is Invalid -> this.e
    else -> persistentListOf()
  }

/**
 * All [Parsed]s' [Errors] if it is [Invalid], or an empty [PersistentList] otherwise.
 */
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

/**
 * Flatmap on [Parsed]. Applies [transform] to [this] if [this] is [Valid], and
 * returns the result.
 */
fun <A, B> Parsed<A>.flatMap(transform: (A) -> Parsed<B>): Parsed<B> = when (this) {
  is Valid -> transform(a)
  is Invalid -> this
}

/**
 * Tests a [Parsed] against [predicate]. If it passes (ie, if true) then it returns [this],
 * otherwise it returns [error]'s [CompilationError]
 *
 * See [Parsed.flatMap]
 */
fun <A> Parsed<A>.validate(predicate: (A) -> Boolean, error: (A) -> CompilationError): Parsed<A> =
  flatMap {
    if (predicate(it))
      it.valid()
    else
      error(it).toInvalidParsed()
  }

/**
 * Tests a [Parsed] against [predicate]. If it passes (ie, if true) then it returns [this],
 * otherwise it returns [error]'s [CompilationError]
 *
 * See [Parsed.flatMap]
 *
 * Overloaded version that takes no lambdas.
 */
fun <A> Parsed<A>.validate(predicate: Boolean, error: CompilationError): Parsed<A> =
  flatMap {
    if (predicate)
      it.valid()
    else
      error.toInvalidParsed()
  }

/**
 * Like a [Validated.map] but for two [Parsed]
 *
 * Combines [fst] and [snd] by mapping both to some [R] by applying [map] if both are [Valid].
 * Otherwise, it returns the errors of both [fst] and [snd] concatenated
 */
inline fun <reified A, reified B, R> combine(
  fst: Parsed<A>,
  snd: Parsed<B>,
  map: (A, B) -> R
): Parsed<R> =
  if (fst is Valid && snd is Valid)
    map(fst.a, snd.a).valid()
  else
    (fst.errors + snd.errors).invalid()

/**
 * Like a [Validated.flatMap] but for two [Parsed].
 *
 * Combines [fst] and [snd] by mapping both to some [R] by applying [map] if both are [Valid].
 * Otherwise, it returns the errors of both [fst] and [snd] concatenated
 */
inline fun <reified A, reified B, R> flatCombine(
  fst: Parsed<A>,
  snd: Parsed<B>,
  map: (A, B) -> Parsed<R>
): Parsed<R> =
  if (fst is Valid && snd is Valid)
    map(fst.a, snd.a)
  else
    (fst.errors + snd.errors).invalid()

/**
 * [combine] but with the first argument as a receiver instead of a parameter.
 */
inline fun <reified A, reified B, R> Parsed<A>.combineWith(other: Parsed<B>, map: (A, B) -> R) =
  combine(this, other, map)

class Position(private val l: Int, private val col: Int) {
  override fun toString(): String = "at $l:$col"
}
