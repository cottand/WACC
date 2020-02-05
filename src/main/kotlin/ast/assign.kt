package ic.org.ast

// <assign-lhs>
sealed class AssLHS {
  abstract val type: Type
}

data class IdentLHS(val variable: Variable) : AssLHS() {
  override val type = variable.type
}

data class ArrayElemLHS(val indices: List<Expr>, val variable: Variable) : AssLHS() {
  val ident = variable.ident
  override val type: Type
    get() {
      require(variable.type is ArrayT)
      // Safe cast because caller validated that only arrays are accessed
      return (variable.type as ArrayT).nthNestedType(indices.size)
    }
}

data class PairElemLHS(val pairElem: PairElem, val variable: Variable, val pairs: PairT) :
  AssLHS() {
  override val type = when (pairElem) {
    is Fst -> pairs.fstT
    is Snd -> pairs.sndT
  }
}

sealed class AssRHS {
  abstract val type: Type
}

data class ExprRHS(val expr: Expr) : AssRHS() {
  override val type = expr.type
  override fun toString() = expr.toString()
}

/**
 * If the array literal is empty, [arrT] is [EmptyArrayT]
 * Caller should verify content.
 */
data class ArrayLit(val exprs: List<Expr>, val arrT: AnyArrayT) : AssRHS() {
  override val type = arrT
  override fun toString() =
    exprs.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.toString() }
}

data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS() {
  override val type = PairT(expr1.type, expr2.type)
  override fun toString() = "newpair($expr1, $expr2)"
}

data class PairElemRHS(val pairElem: PairElem, val pairs: PairT) : AssRHS() {
  override val type = when (pairElem) {
    is Fst -> pairs.fstT
    is Snd -> pairs.sndT
  }

  override fun toString() = pairElem.toString()
}

data class Call(val func: FuncIdent, val args: List<Expr>) : AssRHS() {
  override val type = func.retType
  val name = func.name
  override fun toString() = "call ${func.name.name} (..)"
}
