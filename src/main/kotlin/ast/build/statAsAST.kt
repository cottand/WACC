package ic.org.ast.build

import antlr.WACCParser.*
import arrow.core.Validated.Valid
import arrow.core.combine
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.*
import ic.org.util.*
import kotlinx.collections.immutable.plus

internal fun StatContext.asAst(scp: Scope): Parsed<Stat> = when (this) {
  is SkipContext -> Skip(scp, startPosition).valid()

  is AssignContext -> flatCombine(
    assign_lhs().asAst(scp),
    assign_rhs().asAst(scp)
  ) { lhs, rhs ->
    Assign(lhs, rhs, scp, startPosition).valid()
  }.validate({ (lhs, rhs, _) -> lhs.type.matches(rhs.type) },
    { TypeError(startPosition, it.lhs.type, it.rhs.type, "assignment", it.rhs) })

  is DeclareContext -> assign_rhs().asAst(scp).flatMap { rhs ->

    fun inferPairsFromRhs(lhs: PairT, rhs: PairT): PairT {
      val lhsFst = if (lhs.fstT == AnyPairTs()) rhs.fstT else lhs.fstT
      val lhsSnd = if (lhs.sndT == AnyPairTs()) rhs.sndT else lhs.sndT
      return PairT(lhsFst, lhsSnd)
    }

    val lhsType = type().asAst()
    // Speical case: if the type of the LHS is AnyPairTs, we have to determine the actual type
    // of the variable by looking at the RHS.
    // When rhs is a null PairLit, its type is AnyPairTs
    val lhsTypeInferred = when {
      lhsType == AnyPairTs() && rhs.type is AnyPairTs -> rhs.type
      lhsType is PairT && rhs.type is PairT -> inferPairsFromRhs(lhsType, rhs.type as PairT)
      else -> lhsType
    }
    scp.addVariable(startPosition, lhsTypeInferred, Ident(ID()))
      // If RHS is empty array, we match any kind of array on the LHS (case of int[] a = [])
      .validate({ lhs -> lhs.type.matches(rhs.type) },
        { TypeError(startPosition, it.type, rhs.type, "declaration", rhs) })
      .map { Decl(it, rhs, scp, startPosition) }
  }

  is ReadStatContext -> assign_lhs().asAst(scp)
    .validate({ it.type is IntT || it.type is StringT || it.type is CharT },
      {
        val supported = listOf(IntT, StringT, CharT)
        TypeError(assign_lhs().startPosition, supported, it.type, "read")
      })
    .map { Read(it, scp, startPosition) }

  is FreeStatContext -> expr().asAst(scp)
    // FREE may only be called in expressions that evaluate to types PairT or ArrayT
    .validate({ it.type is AnyPairTs || it.type is AnyArrayT },
      { expr ->
        val supported = listOf(AnyArrayT(), AnyPairTs())
        TypeError(startPosition, supported, expr.type, "Free", expr)
      })
    .map { Free(it, scp, startPosition) }

  is ReturnStatContext -> expr().asAst(scp)
    .validate(scp !is GlobalScope, InvalidReturn(startPosition))
    .map { Return(it, scp, startPosition) }

  is ExitStatContext -> expr().asAst(scp)
    .validate(
      { it.type.matches(IntT) },
      { TypeError(expr().startPosition, IntT, "exit", it) })
    .map { Exit(it, scp, startPosition) }

  is PrintlnStatContext -> expr().asAst(scp).map { Println(it, scp, startPosition) }

  is PrintStatContext -> expr().asAst(scp).map { Print(it, scp, startPosition) }

  is IfElseContext -> asAst(scp)

  is WhileDoContext -> asAst(scp)

  is NewScopeContext -> ControlFlowScope(scp).let { newScope ->
    stat().asAst(newScope).map { BegEnd(it, scp, startPosition) }
  }

  is SemiColonContext -> asAst(scp)
  else -> NOT_REACHED()
}

fun WhileDoContext.asAst(scope: Scope) = ControlFlowScope(scope).let { newScope ->
  expr().asAst(scope)
    .validate(
      { it.type is BoolT },
      { TypeError(startPosition, BoolT, "While condition", it) })
    .combineWith(stat().asAst(newScope)) { cond, body ->
      While(cond, body, scope, startPosition)
    }
}

fun IfElseContext.asAst(scope: Scope): Parsed<If> {
  val thenScope = ControlFlowScope(scope)
  val elseScope = ControlFlowScope(scope)
  val cond = expr().asAst(scope)
    .validate(
      { it.type is BoolT },
      { TypeError(startPosition, BoolT, "If condition", it) }
    )
  val then = stat(0).asAst(thenScope)
  val `else` = stat(1).asAst(elseScope)
  return if (cond is Valid && then is Valid && `else` is Valid)
    If(cond.a, then.a, `else`.a, scope, startPosition).valid()
  else
    (cond.errors + then.errors + `else`.errors).invalid()
}

fun SemiColonContext.asAst(scope: Scope): Parsed<StatChain> {
  // In a stat chain, we should only have two statements
  assert(stat().size == 2)
  // Make sure the two statements are valid
  val stat1 = stat()[0].asAst(scope)
  val stat2 = stat()[1].asAst(scope)

  val statChain = if (stat1 is Valid && stat2 is Valid)
    StatChain(stat1.a, stat2.a, scope, startPosition).valid()
  else
    (stat1.errors + stat2.errors).invalid()

  return statChain
    // It is a semantic error to have a return statement be followed by junk.
    .validate(
      { it.thisStat !is Return },
      { ControlFlowTypeError(startPosition, it.nextStat.toString()) })
    // It is also an error to have an exit statement followed by junk, unless we are not in a
    // function.
    .validate(
      { it.thisStat !is Exit || scope is GlobalScope },
      { ControlFlowTypeError(startPosition, it.nextStat.toString()) })
}
