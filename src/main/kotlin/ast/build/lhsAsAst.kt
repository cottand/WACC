package ic.org.ast.build

import antlr.WACCParser.ArrayOfArraysContext
import antlr.WACCParser.ArrayOfBaseTContext
import antlr.WACCParser.ArrayOfPairsContext
import antlr.WACCParser.Array_typeContext
import antlr.WACCParser.Assign_lhsContext
import antlr.WACCParser.Base_typeContext
import antlr.WACCParser.BoolBaseTContext
import antlr.WACCParser.CharBaseTContext
import antlr.WACCParser.IntBaseTContext
import antlr.WACCParser.LHSArrayElemContext
import antlr.WACCParser.LHSIdentContext
import antlr.WACCParser.LHSPairElemContext
import antlr.WACCParser.Pair_typeContext
import antlr.WACCParser.StringBaseTContext
import antlr.WACCParser.TypeContext
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.AnyArrayT
import ic.org.ast.AnyPairTs
import ic.org.ast.ArrayElemLHS
import ic.org.ast.ArrayT
import ic.org.ast.AssLHS
import ic.org.ast.BaseT
import ic.org.ast.BoolT
import ic.org.ast.CharT
import ic.org.ast.IdentLHS
import ic.org.ast.IntT
import ic.org.ast.PairElem
import ic.org.ast.PairElemLHS
import ic.org.ast.PairT
import ic.org.ast.Scope
import ic.org.ast.StringT
import ic.org.ast.Type
import ic.org.util.NOT_REACHED
import ic.org.util.Parsed
import ic.org.util.TypeError
import ic.org.util.UndefinedIdentifier
import ic.org.util.areAllValid
import ic.org.util.errors
import ic.org.util.flatMap
import ic.org.util.position
import ic.org.util.startPosition
import ic.org.util.validate
import ic.org.util.valids

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
      {
        TypeError(
          array_elem().ID().position,
          AnyArrayT(),
          it.variable.type,
          "array element access"
        )
      })

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
