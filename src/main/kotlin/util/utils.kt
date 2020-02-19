@file:Suppress("DataClassPrivateConstructor", "NOTHING_TO_INLINE")

package ic.org.util

import arrow.core.Either
import arrow.core.Validated
import arrow.core.Validated.Valid
import arrow.core.extensions.list.foldable.forAll
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * Returns whether a [List] of [Either] contains any [Either.Left]
 */
fun <A, B> List<Either<A, B>>.containsLefts() = !this.forAll { it.isRight() }

/**
 * Returns whether a [List] of [Either] contains any [Either.Right]s
 */
fun <A, B> List<Either<A, B>>.containsRights() = !this.forAll { it.isLeft() }

/**
 * Prints [this], while returning [this]. Useful for [println] debugging.
 */
inline fun <reified T> T.print(str: (T) -> String) = this.also { println(str(this)) }

@Suppress("FunctionName")
fun NOT_REACHED(): Nothing = throw IllegalStateException("Case should never be reached")

/**
 * Returns this [TerminalNode]'s position in the program by looking at its
 * [org.antlr.v4.runtime.Token]
 */
inline val TerminalNode.position
  get() = Position(symbol.line, symbol.charPositionInLine + 1)

/**
 * Returns this [ParserRuleContext]'s start position in the program by looking at its
 * [org.antlr.v4.runtime.Token]
 */
inline val ParserRuleContext.startPosition
  get() = Position(start.line, start.charPositionInLine + 1)

/**
 * Returns every element in this [List] of [Validated] that happens to be [Valid].
 */
inline val <reified E, reified V> List<Validated<E, V>>.valids
  get() = this.filterIsInstance<Valid<V>>().map { it.a }
