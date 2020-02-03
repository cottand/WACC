package ic.org.ast

import antlr.WACCParser.*
import arrow.core.*
import arrow.core.Validated.Valid
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.plus

internal fun StatContext.asAst(scope: Scope): Parsed<Stat> = when (this) {
  is SkipContext -> Skip(scope).valid()

  is AssignContext -> {
    val lhs = assign_lhs().asAst(scope)
    val rhs = assign_rhs().asAst(scope)

    flatCombine(lhs, rhs) { lhs, rhs ->
      if (lhs.type == rhs.type)
        Assign(lhs, rhs, scope).valid()
      else
        TypeError(startPosition, lhs.type, rhs.type, "assignment").toInvalidParsed()
    }
  }

  is DeclareContext -> flatCombine(assign_rhs().asAst(scope), type().asAst()) { rhs, lhsType ->
    // Speical case: if the type of the LHS is AnyPairTs, we have to determine the actual type
    // of the variable by looking at the RHS.
    // When rhs is a null PairLit, its type is AnyPairTs
    val lhsTypeInferred =
      if (lhsType == AnyPairTs() && rhs.type is PairT)
        rhs.type
      else
        lhsType

    DeclVariable(lhsTypeInferred, Ident(ID()), rhs)
      .valid()
      .flatMap { scope.addVariable(startPosition, it) }
      // If RHS is empty array, we match any kind of array on the LHS (case of int[] a = [])
      .validate({ lhsType == rhs.type || lhsType is AnyArrayT && rhs.type == EmptyArrayT() },
        { TypeError(startPosition, rhs.type, it.type, "declaration") })
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
