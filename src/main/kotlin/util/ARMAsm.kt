package ic.org.util

import ic.org.arm.Data
import ic.org.arm.Exception
import ic.org.arm.ARMAsmInstr
import ic.org.arm.StdFunc
import ic.org.arm.StringData
import java.util.LinkedList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList

typealias Instructions = PersistentList<ARMAsmInstr>
typealias Datas = PersistentList<Data>

/**
 * Returned by something that produces assembly. [instr] corresponds to the assembly instructions,
 * and [data] to information in the Data segment.
 */
class ARMAsm
private constructor(
  val instr: Instructions = persistentListOf(),
  val data: Datas = persistentListOf(),
  val funcs: PersistentSet<ARMAsm> = persistentSetOf()
) {
  val isEmpty by lazy { instr.isEmpty() && data.isEmpty() && funcs.isEmpty() }

  constructor(instr: Instructions = persistentListOf(), data: Datas = persistentListOf()) :
    this(instr, data, persistentSetOf())

  fun combine(other: ARMAsm) = ARMAsm(instr + other.instr, data + other.data, funcs.addAll(other.funcs))

  operator fun plus(other: ARMAsm) = combine(other)
  operator fun plus(other: Instructions) = combine(ARMAsm(other))
  operator fun plus(other: ARMAsmInstr) = combine(instr(other))

  fun withFunction(other: ARMAsm) =
    ARMAsm(instr, data, funcs + other).withFunctions(other.funcs)

  fun withFunctions(others: Collection<ARMAsm>) =
    ARMAsm(instr, data, funcs + others + others.map { it.funcs }.flatten())

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as ARMAsm
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

    fun write(init: BuilderScope.() -> Unit) =
      BuilderScope().apply(init).codes.flatten()

    val empty = ARMAsm(
      persistentListOf<Nothing>(),
      persistentListOf<Nothing>()
    )

    fun instr(instr: ARMAsmInstr) = ARMAsm(
      persistentListOf(instr),
      persistentListOf<Nothing>()
    )
  }

  class BuilderScope {
    internal val codes = LinkedList<ARMAsm>()
    operator fun ARMAsm.unaryPlus() = codes.addLast(this)
    operator fun ARMAsmInstr.unaryPlus() = codes.addLast(ARMAsm(instr = persistentListOf(this)))
    operator fun List<ARMAsmInstr>.unaryPlus() = codes.addLast(ARMAsm(instr = toPersistentList()))
    fun withFunction(other: ARMAsm) = codes.addLast(empty.withFunction(other))
    fun withFunction(exception: Exception) = withFunction(exception.body)
    fun withFunction(func: StdFunc) = withFunction(func.body)
    fun withFunctions(others: Collection<ARMAsm>) = codes.addLast(empty.withFunctions(others))
    fun data(init: BuilderDataScope.() -> Unit) {
      val d = BuilderDataScope().apply(init)
      codes.addLast(ARMAsm(data = d.instrs.toPersistentList()))
    }
  }

  class BuilderDataScope {
    internal val instrs = LinkedList<Data>()
    operator fun Data.unaryPlus() = instrs.addLast(this)
    operator fun StringData.unaryPlus() = body.forEach { instrs.addLast(it) }
    operator fun List<Data>.unaryPlus() = forEach { instrs.addLast(it) }
  }
}

fun Instructions.code() = ARMAsm(this)
fun List<ARMAsm>.flatten() = fold(ARMAsm.empty, ARMAsm::combine)
