package ic.org

import arrow.core.Either
import arrow.core.extensions.list.foldable.forAll
import arrow.core.right
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

fun <A, B> List<Either<A, B>>.containsLefts() = !this.forAll { it.isRight() }
fun <A, B> List<Either<A, B>>.containsRights() = !this.forAll { it.isLeft() }

fun <A, B> List<Either<A, B>>.findFirstLeft(): Either<A, List<B>> {
  for (e in this) {
    if (e is Either.Left) return e
  }
  return this.filterIsInstance<Either.Right<B>>().map { e -> e.b }.right()

}

inline fun <reified A, reified B> PersistentList<A>.mapp(transform: (A) -> B) =
  this.map(transform).toPersistentList()
