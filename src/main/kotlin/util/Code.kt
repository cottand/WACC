package ic.org.util

import ic.org.arm.Data
import ic.org.arm.Instr
import kotlinx.collections.immutable.*

typealias Instructions = PersistentList<Instr>
typealias Datas = PersistentList<Data>

/**
 * Returned by something that produces assembly. [instr] corresponds to the assembly instructions,
 * and [data] to information in the Data segment.
 */
class Code
private constructor(
  val instr: Instructions = persistentListOf(),
  val data: Datas = persistentListOf(),
  private val funcs: PersistentSet<Code> = persistentSetOf()
) {
  val isEmpty by lazy { instr.isEmpty() && data.isEmpty() && funcs.isEmpty() }

  constructor(instr: Instructions = persistentListOf(), data: Datas = persistentListOf())
    : this(instr, data, persistentSetOf())

  fun combine(other: Code) = Code(instr + other.instr, data + other.data)
    .withFunctions(funcs + other.funcs)

  operator fun plus(other: Code) = combine(other)
  operator fun plus(other: Instructions) = combine(Code(other))
  operator fun plus(other: Instr) = combine(instr(other))

  fun withFunction(other: Code) =
    Code(instr, data, funcs + other + other.funcs)

  fun withFunctions(others: Collection<Code>) =
    Code(instr, data, funcs + others.toPersistentList() + others.map { it.funcs }.flatten())

  val functions by lazy { funcs.fold(empty, Code::combine) }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Code
    if (instr != other.instr) return false
    if (data != other.data) return false
    if (funcs != other.funcs) return false
    return true
  }

  override fun hashCode(): Int {
    var result = instr.hashCode()
    result = 31 * result + data.hashCode()
    result = 31 * result + funcs.hashCode()
    return result
  }

  operator fun component1() = instr
  operator fun component2() = data

  override fun toString() = "Code(instr=\n$instr, data=\n$data, funcs=\n$funcs)"

  companion object {
    val empty = Code(
      persistentListOf<Nothing>(),
      persistentListOf<Nothing>()
    )

    fun instr(instr: Instr) = Code(
      persistentListOf(instr),
      persistentListOf<Nothing>()
    )
  }
}

fun Instructions.code() = Code(this)
fun List<Code>.flatten() = fold(Code.empty, Code::combine)