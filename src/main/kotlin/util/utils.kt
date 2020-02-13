package ic.org.util

import arrow.core.*
import arrow.core.Validated.Valid
import arrow.core.extensions.list.foldable.forAll
import arrow.syntax.collections.tail
import ic.org.arm.Data
import ic.org.arm.Instr
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
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
 * Returns whether [this] contains every word in [words]
 */
fun String.containsAll(words: List<String>, ignoreCase: Boolean = true) =
  words.forAll { this.contains(it, ignoreCase) }

/**
 * Prints [this], while returning [this]. Useful for [println] debugging.
 */
inline fun <reified T> T.print() = this.also { print(it) }

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

inline fun <A> Option<A>.ifExsists(run: (A) -> Unit) {
  if (this is Some) run(this.t)
}
typealias Instructions = PersistentList<Instr>
typealias Datas = PersistentList<Data>

/**
 * Returned by something that produces assembly. [instr] corresponds to the assembly instructions,
 * and [data] to information in the Data segment.
 */
@Suppress("NOTHING_TO_INLINE")
data class Code(val instr: Instructions = persistentListOf(), val data: Datas = persistentListOf()) {

  inline fun combine(other: Code) = Code(instr + other.instr, data + other.data)
  inline operator fun plus(other: Code) = combine(other)
  inline operator fun plus(other: Instructions) = combine(Code(other))
  inline operator fun plus(other: Instr) = combine(Code(persistentListOf(other)))

  companion object {
    val empty = Code(persistentListOf<Nothing>(), persistentListOf<Nothing>())
  }
}

fun List<Code>.flatten() = fold(Code.empty, Code::combine)

fun <A, B> List<A>.mapp(transform: (A) -> B) = map(transform).toPersistentList()

val <E> PersistentList<E>.head
  get() = take(1).firstOrNone()

fun <E> PersistentList<PersistentList<E>>.flatten(): PersistentList<E> {
  if (head is None) return persistentListOf<Nothing>()
  var total: PersistentList<E> = (head as Some<PersistentList<E>>).t
  for (l in this.tail()) {
    total += l
  }
  return total
}

fun Instructions.code() = Code(this)


