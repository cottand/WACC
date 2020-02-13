package ic.org.util

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.extensions.list.foldable.forAll
import arrow.core.firstOrNone
import arrow.syntax.collections.tail
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Like [List.map], except it produces a [PersistentList]
 */
fun <A, B> List<A>.mapp(transform: (A) -> B) = map(transform).toPersistentList()

val <E> PersistentList<E>.head
  get() = take(1).firstOrNone()

/**
 * Like List flattening but for [PersistentList]
 */
fun <E> PersistentList<PersistentList<E>>.flatten(): PersistentList<E> {
  if (head is None) return persistentListOf<Nothing>()
  var total: PersistentList<E> = (head as Some<PersistentList<E>>).t
  for (l in this.tail()) {
    total += l
  }
  return total
}

val <A> List<A>.head
  get() = first()

val <A> List<A>.tail
  get() = tail()

fun List<String>.joinLines() = joinToString(separator = "\n")
fun Stream<String>.joinLines() = toList().joinToString(separator = "\n")

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
 * Runs [run] on [this.t] if [this] is [Some]
 */
inline fun <A> Option<A>.ifExsists(run: (A) -> Unit) {
  if (this is Some) run(this.t)
}

/**
 * Runs [run] on [this.t] if [this] is [Some] and [pred] is true
 */
inline fun <A> Option<A>.ifExsistsAnd(pred: Boolean, run: (A) -> Unit): Option<A> {
  if (this is Some && pred) run(this.t)
  return this
}
