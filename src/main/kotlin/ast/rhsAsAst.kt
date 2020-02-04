package ic.org.ast

import antlr.WACCParser.Assign_rhsContext
import arrow.core.*
import arrow.core.extensions.list.monadFilter.filterMap
import arrow.core.extensions.list.zip.zipWith
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.toPersistentList

internal fun Assign_rhsContext.asAst(scope: Scope): Parsed<AssRHS> {
  when {
    array_lit() != null -> {
      val tokExprs = array_lit().expr()
      if (tokExprs.isEmpty()) {
        return ArrayLit(emptyList(), EmptyArrayT()).valid()
      }

      // Transform all the expressions to ASTs
      val exprs = tokExprs.map { it.asAst(scope) }
      if (!exprs.areAllValid) {
        return exprs.errors.invalid()
      }
      val valid = exprs.valids

      // Make sure expressions all have the same type
      val t = valid[0].type
      for (e in valid) {
        if (e.type != t) {
          return TypeError(
            tokExprs[0].startPosition,
            t,
            e.type,
            "array building"
          )
            .toInvalidParsed()
        }
      }
      val arrayT = ArrayT.make(valid.first().type)
      return ArrayLit(valid, arrayT).valid()
    }
    NEWPAIR() != null -> return combine(
      expr(0).asAst(scope),
      expr(1).asAst(scope)
    ) { e1, e2 ->
      Newpair(e1, e2)
    }
    pair_elem() != null ->
      // TODO PICKUP WHAT ABOUT NESTED PAIRS AND ARRAYS
      return pair_elem().expr().asAst(scope)
        .validate(
          { it !is NullPairLit }) {
          NullPairError(pair_elem().expr().startPosition)
        }
        .validate({ it.type is PairT }) {
          TypeError(
            pair_elem().expr().startPosition,
            AnyPairTs(),
            it.type,
            "pair-elem"
          )
        }
        .map {
          // Safe cast because it was checked in the above validate() clause
          val pairT = it.type as PairT
          if (pair_elem().FST() != null)
            PairElemRHS(
              Fst(it),
              pairT
            )
          else
            PairElemRHS(
              Snd(it),
              pairT
            )
        }

    CALL() != null -> {
      // Make ASTs out of all the args
      val args: List<Parsed<Expr>> =
        arg_list()?.expr().toOption().getOrElse { emptyList() }
          .map { it.asAst(scope) }
      if (!args.areAllValid) {
        return args.errors.invalid()
      }

      // TODO make sure arg types are good
      // TODO check startPosition etc

      val id = ID().text
      return scope.globalFuncs[id].toOption()
        .fold(
          { UndefinedIdentifier(ID().position, id).toInvalidParsed() },
          { func ->
            Call(func, args.valids).valid()
          })
        .validate(
          { it.func.params.size == it.args.size },
          { UnexpectedNumberOfParamsError(startPosition, it.name, it.func.params.size, it.args.size) })
        .flatMap { call ->
          val argParamZip = call.args.zipWith(call.func.params) { a, p -> a to p }
          val mismatches = argParamZip.filterMap { (a, p) ->
            if (!p.type.matches(a.type))
              TypeError(startPosition, p.type, a.type, "parameter passing").some()
            else None
          }.toPersistentList()

          if (mismatches.isNotEmpty())
            mismatches.invalid()
          else
            call.valid()
        }
    }

    expr() != null -> {
      return expr(0).asAst(scope).map { ExprRHS(it) }
    }
    else -> NOT_REACHED()
  }
}
