package ic.org.util

import ic.org.arm.*
import kotlinx.collections.immutable.*
import java.util.LinkedList

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
  val funcs: PersistentSet<Code> = persistentSetOf()
) {
  val isEmpty by lazy { instr.isEmpty() && data.isEmpty() && funcs.isEmpty() }

  constructor(instr: Instructions = persistentListOf(), data: Datas = persistentListOf())
    : this(instr, data, persistentSetOf())

  fun combine(other: Code) = Code(instr + other.instr, data + other.data, funcs + other.funcs)

  operator fun plus(other: Code) = combine(other)
  operator fun plus(other: Instructions) = combine(Code(other))
  operator fun plus(other: Instr) = combine(instr(other))

  fun withFunction(other: Code) =
    Code(instr, data, funcs + other).withFunctions(other.funcs)

  fun withFunctions(others: Collection<Code>) =
    Code(instr, data, funcs + others + others.map { it.funcs }.flatten())

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

  override fun toString() =
    "Code(instr=\n${instr.map { it.code }.joinLines()}, data=\n$data, funcs=\n$funcs)"

  companion object {

    fun write(init: CodeBuilderScope.() -> Unit)  =
      CodeBuilderScope().apply(init).codes.flatten()

    val empty = Code(
      persistentListOf<Nothing>(),
      persistentListOf<Nothing>()
    )

    fun instr(instr: Instr) = Code(
      persistentListOf(instr),
      persistentListOf<Nothing>()
    )
  }

  class CodeBuilderScope {
    internal val codes = LinkedList<Code>()
    operator fun Code.unaryPlus() = codes.push(this)
    operator fun Instr.unaryPlus() = codes.push(Code(instr = persistentListOf(this)))
    operator fun List<Instr>.unaryPlus() = codes.push(Code(instr = toPersistentList()))
    fun withFunction(other: Code) = codes.push(empty.withFunctions(other.funcs))
    fun withFunction(exception: Exception) = withFunction(exception.body)
    fun withFunction(func: StdFunc) = withFunction(func.body)
    fun withFunctions(others: Collection<Code>) = codes.push(empty.withFunctions(others))
    fun data(init: CodeBuilderDataScope.() -> Unit) {
      val d = CodeBuilderDataScope().apply(init)
      codes.push(Code(data = d.instrs.toPersistentList()))
    }
  }

  class CodeBuilderDataScope {
    internal val instrs = LinkedList<Data>()
    operator fun Data.unaryPlus() = instrs.push(this)
    operator fun StringData.unaryPlus() = body.forEach { instrs.push(it) }
    operator fun List<Data>.unaryPlus() = forEach { instrs.push(it) }
  }
}

fun Instructions.code() = Code(this)
fun List<Code>.flatten() = fold(Code.empty, Code::combine)