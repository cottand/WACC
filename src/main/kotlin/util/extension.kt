package ic.org.util

import arrow.core.*
import arrow.core.extensions.list.foldable.forAll
import arrow.syntax.collections.tail
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import java.io.File
import java.util.UUID
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Like [List.map], except it produces a [PersistentList]
 */
fun <A, B> List<A>.mapp(transform: (A) -> B) = map(transform).toPersistentList()

val <E> PersistentList<E>.headOrNone
  get() = take(1).firstOrNone()

infix fun <E> E.prepend(list: PersistentList<E>) = list.add(0, this)

/**
 * Like List flattening but for [PersistentList]
 */
fun <E> PersistentList<PersistentList<E>>.flatten(): PersistentList<E> {
  if (headOrNone is None) return persistentListOf<Nothing>()
  var total: PersistentList<E> = (headOrNone as Some<PersistentList<E>>).t
  for (l in this.tail()) {
    total += l
  }
  return total
}

val <A> List<A>.head
  get() = first()

val <A> List<A>.tail
  get() = tail().toPersistentList()

fun Iterable<String>.joinLines() = joinToString(separator = "\n")
fun Sequence<String>.joinLines() = joinToString(separator = "\n")
fun Stream<String>.joinLines() = toList().joinToString(separator = "\n")

/**
 * Returns whether [this] contains every word in [words]
 */
fun String.containsAll(words: List<String>, ignoreCase: Boolean = true) =
  words.forAll { this.contains(it, ignoreCase) }

/**
 * Prints [this], while returning [this]. Useful for [println] debugging.
 */
inline fun <reified T> T.print() = this.also {  println("Printing ${T::class.java}: $it") }

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

val <E> PersistentList<E>.take2: Triple<E, E, PersistentList<E>>
  inline get() {
    val (fst, snd) = this
    val rest = this.removeAt(0).removeAt(0)
    return Triple(fst, snd, rest)
  }

val <E> PersistentList<E>.take2OrNone
  inline get() = if (this.size >= 2) take2.some() else None

fun shortRandomUUID() = UUID.randomUUID().toString().take(8)

fun File.createWithDirs() {
  parentFile.mkdirs()
  createNewFile()
}

