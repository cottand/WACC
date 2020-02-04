package ic.org.ast

import antlr.WACCParser.*
import arrow.core.invalid
import arrow.core.valid
import ic.org.*
import ic.org.grammar.*

internal fun TypeContext.asAst(): Type =
  when {
    base_type() != null -> base_type().asAst()
    pair_type() != null -> pair_type().asAst()
    array_type() != null -> array_type().asAst()
    else -> NOT_REACHED()
  }

internal fun Base_typeContext.asAst(): BaseT = when (this) {
  is IntBaseTContext -> IntT
  is BoolBaseTContext -> BoolT
  is CharBaseTContext -> CharT
  is StringBaseTContext -> StringT
  else -> NOT_REACHED()
}

private fun Pair_typeContext.asAst(): AnyPairTs {
  val fstType = pair_elem_type(0).asAst()
  val sndType = pair_elem_type(1).asAst()
  return PairT(fstType, sndType)
}

internal fun Array_typeContext.asAst(): ArrayT {
  fun recurseArrayT(arrayT: Array_typeContext, currentDepth: Int): Pair<Type, Int> =
    when (arrayT) {
      is ArrayOfArraysContext -> recurseArrayT(arrayT.array_type(), currentDepth + 1)
      is ArrayOfBaseTContext -> arrayT.base_type().asAst() to currentDepth
      is ArrayOfPairsContext -> arrayT.pair_type().asAst() to currentDepth
      else -> NOT_REACHED()
    }
  val (type, depth) = recurseArrayT(this, 1)
  return ArrayT.make(type, depth)
}

internal fun Assign_lhsContext.asAst(scope: Scope): Parsed<AssLHS> = when (this) {
  is LHSIdentContext -> scope[ID().text].fold({
    UndefinedIdentifier(startPosition, ID().text).toInvalidParsed()
  }, { IdentLHS(it).valid() })

  is LHSArrayElemContext -> scope[array_elem().ID().text].fold({
    UndefinedIdentifier(startPosition, array_elem().text).toInvalidParsed()
  }, { variable ->
    val exprs = array_elem().expr().map { it.asAst(scope) }
    if (exprs.areAllValid)
      ArrayElemLHS(exprs.valids, variable).valid()
    else
      exprs.errors.invalid()
  })
    .validate({ it.variable.type is AnyArrayT },
      { TypeError(array_elem().ID().position, AnyArrayT(), it.variable.type, "array element access") })

  is LHSPairElemContext -> pair_elem().expr().asAst(scope)
    .validate(
      { it.type is PairT },
      { TypeError(startPosition, AnyPairTs(), it.type, pair_elem().text) })
    .flatMap { pairIdent ->
      scope[pair_elem().expr().text].fold({
        UndefinedIdentifier(startPosition, pair_elem().text).toInvalidParsed()
      }, { vari ->
        val elem =
          if (pair_elem().FST() != null)
            PairElem.fst(pairIdent)
          else
            PairElem.snd(pairIdent)
        // vari.type as PairT is a safe cast because it was checked in the validate() clause.
        PairElemLHS(elem, vari, vari.type as PairT).valid()
      })
    }

  else -> NOT_REACHED()
}
