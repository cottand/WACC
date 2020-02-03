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

    if (lhs is Valid && rhs is Valid)
      Assign(lhs.a, rhs.a, scope).valid()
    else
      (lhs.errors + rhs.errors).invalid()
  }

  is DeclareContext -> assign_rhs().asAst(scope).flatMap { rhs ->
    type().asAst()
      .map { DeclVariable(it, Ident(ID()), rhs) }
      .flatMap { scope.addVariable(startPosition, it) }
      .validate ({
        rhs.fetchType(scope).fold({
          false
        }, {
          t -> it.type == t
        })
      }, {
        TypeError(type().startPosition, rhs.fetchType(scope).getOrElse { BoolT }, it.type, "variable declaration")
      })
      .map { Decl(it, rhs, scope) }
  }

  is ReadStatContext -> assign_lhs().asAst(scope)
      .validate ({
        it.fetchType(scope).fold({
          false
        },{ it is IntT || it is StringT || it is CharT })
      }, {
        TypeError(
          assign_lhs().startPosition,
          listOf(IntT, StringT, CharT),
          it.fetchType(scope).getOrElse { "failed to compute LHS expr type" }.toString(),
          "read"
        )
      }).map { Read(it, scope) }

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
