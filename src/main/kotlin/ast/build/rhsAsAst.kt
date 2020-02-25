package ic.org.ast.build

import antlr.WACCParser.*
import arrow.core.getOrElse
import arrow.core.invalid
import arrow.core.toOption
import arrow.core.valid
import ic.org.ast.*
import ic.org.util.*
import kotlinx.collections.immutable.toPersistentList

internal fun Assign_rhsContext.asAst(scp: Scope): Parsed<AssRHS> {
  return when (this) {
    is RHSArrayLitContext -> asAst(scp)
    // Combine both expressions of the newpair into a [Newpair]
    is RHSNewpairContext -> combine(expr(0).asAst(scp), expr(1).asAst(scp)) { e1, e2 ->
      Newpair(e1, e2)
    }

    is RHSPairElemContext -> pair_elem().expr().asAst(scp)
      .validate({ it !is NullPairLit }) {
        NullPairError(pair_elem().expr().startPosition)
      }
      .validate({ it.type is PairT }) {
        TypeError(pair_elem().expr().startPosition, AnyPairTs(), it.type, "pair-elem")
      }
      .map {
        val pairElem = if (pair_elem().FST() != null) PairElem.fst(it)
        else PairElem.snd(it)
        // Safe cast because it was checked in the above validate() clause
        PairElemRHS(pairElem, it.type as PairT)
      }
    is RHSFuncCallContext -> asAst(scp)
    is RHSExprContext -> expr().asAst(scp).map { ExprRHS(it) }
    else -> NOT_REACHED()
  }
}

fun RHSArrayLitContext.asAst(scp: Scope): Parsed<ArrayLit> {
  val tokExprs = array_lit().expr()
  if (tokExprs.isNullOrEmpty()) {
    return ArrayLit(emptyList(), EmptyArrayT()).valid()
  }

  // Transform all the expressions to ASTs
  val exprs = tokExprs.map { it.asAst(scp) }
  if (!exprs.areAllValid) {
    return exprs.errors.invalid()
  }
  val valid = exprs.valids

  // Make sure expressions all have the same type
  val t = valid[0].type
  for (e in valid) {
    if (e.type != t) {
      return TypeError(tokExprs[0].startPosition, t, "array building", e).toInvalidParsed()
    }
  }
  val arrayT = ArrayT.make(valid.first().type)
  return ArrayLit(valid, arrayT).valid()
}

fun RHSFuncCallContext.asAst(scp: Scope): Parsed<Call> {
  // Make ASTs out of all the args
  val args: List<Parsed<Expr>> =
    arg_list()?.expr().toOption().getOrElse { emptyList() }
      .map { it.asAst(scp) }
  if (!args.areAllValid) {
    return args.errors.invalid()
  }

  val id = ID().text
  return scp.globalFuncs[id].toOption()
    // Handle the case where no such function exists
    .fold(
      { UndefinedIdentifier(ID().position, id).toInvalidParsed() },
      { func -> Call(func, args.valids).valid() })
    // Validate the number of arguments passed
    .validate(
      { it.func.params.size == it.args.size },
      { UnexpectedNumberOfParamsError(startPosition, it.name, it.func.params.size, it.args.size) })
    // Make sure that for every type of the arguments passed, it matches the function's. We do this by zipping the two
    // lists, then comparing each element pair-waise.
    .flatMap { call ->
      call.args.zip(call.func.params)
        .filterNot { (a, p) -> p.type.matches(a.type) }
        .map { (a, p) -> TypeError(startPosition, p.type, a.type, "parameter passing") }
        .let { mismatches ->
          if (mismatches.isNotEmpty())
            mismatches.toPersistentList().invalid()
          else
            call.valid()
        }
    }
}
