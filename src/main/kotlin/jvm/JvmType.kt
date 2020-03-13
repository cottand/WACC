package ic.org.jvm

import ic.org.ast.AnyArrayT
import ic.org.ast.AnyPairTs
import ic.org.ast.ArrayT
import ic.org.ast.BoolT
import ic.org.ast.CharT
import ic.org.ast.IntT
import ic.org.ast.StringT
import ic.org.ast.Type

sealed class JvmType {
  abstract val rep: String
  abstract val toNonPrimative: JvmAsm
  abstract val toPrimative: JvmAsm
  override fun toString() = rep
}

fun Type.toJvm(): JvmType = when (this) {
  IntT -> JvmInt
  BoolT -> JvmBool
  CharT -> JvmChar
  StringT -> JvmString
  is ArrayT -> JvmArray(type.toJvm())
  is AnyArrayT -> JvmArray(JvmObject)
  is AnyPairTs -> JvmWaccPair
}

object PrintStream : JvmType() {
  override val rep = "Ljava/io/PrintStream;"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm.empty
}

object Scanner : JvmType() {
  override val rep = "Ljava/util/Scanner;"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm.empty
}

object JvmInputStream : JvmType() {
  override val rep = "Ljava/io/InputStream;"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm.empty
}

object JvmInt : JvmType() {
  override val rep = "I"
  override val toNonPrimative = JvmAsm.instr(InvokeStatic("java/lang/Integer/valueOf(I)Ljava/lang/Integer;"))
  override val toPrimative = JvmAsm {
    +CheckCast("java/lang/Integer")
    +InvokeVirtual("java/lang/Integer/intValue()I")
  }
}

data class JvmArray(val t: JvmType) : JvmType() {
  override val rep = "[$t"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm.empty
}

object JvmObject : JvmType() {
  override val rep = "Ljava/lang/Object;"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm.empty
}

object JvmString : JvmType() {
  override val rep = "Ljava/lang/String;"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm {
    +CheckCast("Ljava/lang/String")
  }
}

object JvmVoid : JvmType() {
  override val rep = "V"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm.empty
}

object JvmBool : JvmType() {
  override val rep = "Z"
  override val toNonPrimative = JvmAsm.instr(InvokeStatic("java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;"))
  override val toPrimative = JvmAsm {
    +CheckCast("java/lang/Boolean")
    +InvokeVirtual("java/lang/Boolean/booleanValue()Z")
  }
}

object JvmChar : JvmType() {
  override val rep = "C"
  override val toNonPrimative = JvmAsm.instr(InvokeStatic("java/lang/Character/valueOf(C)Ljava/lang/Character;"))
  override val toPrimative = JvmAsm {
    +CheckCast("java/lang/Character")
    +InvokeVirtual("java/lang/Character/charValue()C")
  }
}

object JvmWaccPair : JvmType() {
  const val name = "wacc/lang/Pair"
  override val rep = "L$name;"
  override val toNonPrimative = JvmAsm.empty
  override val toPrimative = JvmAsm {
    +CheckCast("wacc/lang/Pair")
  }

  val new = NEW(name)

  class Getter internal constructor(override val mName: String) : JvmMethod(type = Virtual) {
    override val `class` = name
    override val args = listOf<Nothing>()
    override val ret = JvmObject
  }

  val getFst = Getter("getFst")
  val getSnd = Getter("getSnd")

  class Setter internal constructor(override val mName: String) : JvmMethod(type = Virtual) {
    override val `class` = name
    override val args = listOf(JvmObject)
    override val ret = JvmVoid
  }

  val setFst = Setter("setFst")
  val setSnd = Setter("setSnd")

  val init = object : JvmMethod(type = Special) {
    override val `class` = name
    override val mName = "<init>"
    override val args = listOf(JvmObject, JvmObject)
    override val ret = JvmVoid
  }
}

data class CheckCast(val type: String) : JvmInstr {
  override val code = "checkcast $type"
}
