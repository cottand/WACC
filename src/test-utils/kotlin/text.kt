import arrow.core.extensions.list.semialign.padZip
import arrow.core.getOrElse
import ic.org.util.joinLines

fun String.sideToSideWith(other: String, pad: Int = 20) = lines().padZip(other.lines())
  .map { (ol, or) ->
    String.format("%-${pad}s %-${pad}s", ol.getOrElse { "" }, or.getOrElse { "" })
  }.joinLines()
