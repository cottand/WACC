@file:Suppress("DataClassPrivateConstructor", "NOTHING_TO_INLINE")

package ic.org.util

import arrow.core.*
import arrow.core.Validated.Valid
import arrow.core.extensions.list.foldable.forAll
import arrow.syntax.collections.tail
import ic.org.arm.Data
import ic.org.arm.Instr
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.stream.Stream
import kotlin.streams.toList

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


typealias Instructions = PersistentList<Instr>
typealias Datas = PersistentList<Data>

/**
 * Returned by something that produces assembly. [instr] corresponds to the assembly instructions,
 * and [data] to information in the Data segment.
 */
data class Code
private constructor(
  val instr: Instructions = persistentListOf(),
  val data: Datas = persistentListOf(),
  private val funcs: PersistentList<Code> = persistentListOf()
) {

  constructor(instr: Instructions = persistentListOf(), data: Datas = persistentListOf())
    : this(instr, data, persistentListOf())

  inline fun combine(other: Code) = Code(instr + other.instr, data + other.data)
  inline operator fun plus(other: Code) = combine(other)
  inline operator fun plus(other: Instructions) = combine(Code(other))
  inline operator fun plus(other: Instr) = combine(Code.instr(other))

  fun withFunction(other: Code) = Code(instr, data, funcs + other)
  fun withFunctions(others: Collection<Code>) = Code(instr, data, funcs + others.toPersistentList())

  val functions by lazy { funcs.fold(empty, Code::combine) }

  companion object {
    val empty = Code(persistentListOf<Nothing>(), persistentListOf<Nothing>())
    fun instr(instr: Instr) = Code(persistentListOf(instr), persistentListOf<Nothing>())
  }
}

fun Instructions.code() = Code(this)

fun List<Code>.flatten() = fold(Code.empty, Code::combine)



