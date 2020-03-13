import arrow.core.None
import arrow.core.extensions.list.semialign.padZip
import arrow.core.getOrElse
import arrow.core.some
import ic.org.util.joinLines
import ic.org.util.print

fun String.sideToSideWith(other: String, pad: Int = 20) = lines().padZip(other.lines())
  .map { (ol, or) ->
    String.format("%-${pad}s %-${pad}s", ol.getOrElse { "" }, or.getOrElse { "" })
  }
  .mapIndexed { index, s ->
    val lNumber = index.toString()
    lNumber + "     ".drop(lNumber.length) + s
  }
  .joinLines()

/**
 * Reads this [WACCProgram]'s file looking for a particlar input field.
 */
fun WACCProgram.specialInput() = "Input_particular".let { token ->
  file.bufferedReader().lineSequence()
    .takeWhile { "End of $token" !in it }
    .dropWhile { token !in it }
    .drop(1)
    .map { it.drop(2) }
    .joinLines()
    .let { if (it.isBlank()) None else ('\n' + it).some() }.print()
}

/**
 * Prints same text with prepended line numbers
 */
fun Iterable<String>.withLineNumbers() = mapIndexed { i, line ->
  val index = i.toString()
  index + "    ".drop(index.length) + line
}

fun String.withLineNumbers() = lines().withLineNumbers().joinLines()