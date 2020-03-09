@file:Suppress("DataClassPrivateConstructor", "NOTHING_TO_INLINE", "EXPERIMENTAL_API_USAGE")

package ic.org.util

import arrow.core.Either
import arrow.core.None
import arrow.core.Validated
import arrow.core.Validated.Valid
import arrow.core.extensions.list.foldable.forAll
import arrow.core.some
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.File
import kotlin.math.log2

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

inline fun log2(i: Int) =
  log2(i.toDouble()).also { if (it > Byte.MAX_VALUE) throw UnsupportedOperationException() }.toInt().toUByte()

fun String.runCommand(workingDir: File = File("."), input: String = "") =
  ProcessBuilder(*split("\\s".toRegex()).toTypedArray())
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .let { proc ->
      proc.outputStream.bufferedWriter().run {
        write(input + '\n')
        flush()
        close()
      }
      proc.waitFor()
      val errs = proc.errorStream.bufferedReader().readText()
      errs + proc.inputStream.bufferedReader().readText() to proc.exitValue()
    }

inline fun <reified T, reified A : S, reified B : S, reified S> T.unfold(
  pred: (T) -> Boolean,
  ifSo: (T) -> A,
  otherise: (T) -> B
) =
  let { if (pred(this)) ifSo(this) else otherise(this) }

fun String.noneIfEmpy() = unfold(String::isEmpty, { None }, String::some)

