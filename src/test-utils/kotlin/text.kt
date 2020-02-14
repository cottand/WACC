import arrow.core.extensions.list.semialign.padZip
import arrow.core.getOrElse
import ic.org.util.joinLines

fun String.sideToSideWith(other: String, pad: Int = 20) = lines().padZip(other.lines())
  .map { (ol, or) ->
    val l = ol.getOrElse { "" }
    val r = or.getOrElse { "" }
    String.format("%-${pad}s %-${pad}s", l, r)
  }.joinLines()
