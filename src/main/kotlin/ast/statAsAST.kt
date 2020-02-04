package ic.org.ast

import antlr.WACCParser.*
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.plus

internal fun StatContext.asAst(scope: Scope): Parsed<Stat> = when (this) {
  is SkipContext -> Skip(scope).valid()

  is AssignContext -> {
    val lhs = assign_lhs().asAst(scope)
    val rhs = assign_rhs().asAst(scope)

    flatCombine(lhs, rhs) { lhs, rhs ->
      Assign(lhs, rhs, scope).valid()
    }.validate({ (lhs, rhs, _) ->
      lhs.type.matches(rhs.type)
    }, { TypeError(startPosition, it.lhs.type, it.rhs.type, "assignment") })
  }

  is DeclareContext -> assign_rhs().asAst(scope).flatMap { rhs ->

    fun inferPairsFromRhs(lhs: PairT, rhs: PairT): PairT {
      val lhsFst = if (lhs.fstT == AnyPairTs()) rhs.fstT else lhs.fstT
      val lhsSnd = if (lhs.sndT == AnyPairTs()) rhs.sndT else lhs.sndT
      return PairT(lhsFst, lhsSnd)
    }

    val lhsType = type().asAst()
    // Speical case: if the type of the LHS is AnyPairTs, we have to determine the actual type
    // of the variable by looking at the RHS.
    // When rhs is a null PairLit, its type is AnyPairTs
    val lhsTypeInferred =
      when {
        lhsType == AnyPairTs() && rhs.type is AnyPairTs -> rhs.type
        lhsType is PairT && rhs.type is PairT -> inferPairsFromRhs(lhsType, rhs.type as PairT)
        else -> lhsType
      }

    DeclVariable(lhsTypeInferred, Ident(ID()), rhs)
      .valid()
      .flatMap { scope.addVariable(startPosition, it) }
      // If RHS is empty array, we match any kind of array on the LHS (case of int[] a = [])
      .validate({ lhs ->
        lhs.type.matches(rhs.type)
        // || lhs.type is PairT && rhs is ExprRHS && rhs.expr is NullPairLit
      },
        { TypeError(startPosition, it.type, rhs.type, "declaration") })
      .map { Decl(it, rhs, scope) }
  }

  is ReadStatContext -> assign_lhs().asAst(scope)
    .validate({ it.type is IntT || it.type is StringT || it.type is CharT },
      { TypeError(assign_lhs().startPosition, listOf(IntT, StringT, CharT), it.type, "read") })
    .map { Read(it, scope) }

  is FreeStatContext -> expr().asAst(scope)
    // FREE may only be called in expressions that evaluate to types PairT or ArrayT
    .validate({ it.type is AnyPairTs || it.type is AnyArrayT },
      { TypeError(startPosition, listOf(AnyArrayT(), AnyPairTs()), it.type, "Free") })
    .map { Free(it, scope) }

  is ReturnStatContext -> expr().asAst(scope)
    .validate(scope !is GlobalScope, InvalidReturn(startPosition))
    .map { Return(it, scope) }

  is ExitStatContext -> expr().asAst(scope)
    .validate(
      { it.type is IntT },
      { TypeError(expr().startPosition, IntT, it.type, "exit") })
    .map { Exit(it, scope) }

  is PrintlnStatContext -> expr().asAst(scope).map { Print(it, scope) }

  is PrintStatContext -> expr().asAst(scope).map { Println(it, scope) }

  is IfElseContext -> asAst(scope)

  is WhileDoContext -> asAst(scope)

  is NewScopeContext -> ControlFlowScope(scope).let { newScope ->
    stat().asAst(newScope).map { BegEnd(it, newScope) }
  }

  is SemiColonContext -> asAst(scope)
  else -> NOT_REACHED()
}

fun WhileDoContext.asAst(scope: Scope): Parsed<While> {
  val newScope = ControlFlowScope(scope)
  val cond = expr().asAst(scope)
    .validate(
      { it.type is BoolT },
      { TypeError(startPosition, BoolT, it.type, "While condition") })
  val s = stat().asAst(newScope)
  return if (cond is Valid && s is Valid)
    While(cond.a, s.a, newScope).valid()
  else
    (cond.errors + s.errors).invalid()
}

fun IfElseContext.asAst(scope: Scope): Parsed<If> {
  val thenScope = ControlFlowScope(scope)
  val elseScope = ControlFlowScope(scope)
  val cond = expr().asAst(scope)
    .validate(
      { it.type is BoolT },
      { TypeError(startPosition, BoolT, it.type, "If condition") }
    )
  val then = stat(0).asAst(thenScope)
  val `else` = stat(1).asAst(elseScope)
  return if (cond is Valid && then is Valid && `else` is Valid)
    If(cond.a, then.a, `else`.a, scope).valid()
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
    StatChain(stat1.a, stat2.a, scope).valid()
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
