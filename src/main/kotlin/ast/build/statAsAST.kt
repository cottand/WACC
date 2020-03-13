package ic.org.ast.build

import antlr.WACCParser.AssignContext
import antlr.WACCParser.DeclareContext
import antlr.WACCParser.ExitStatContext
import antlr.WACCParser.ForDoContext
import antlr.WACCParser.FreeStatContext
import antlr.WACCParser.IfElseContext
import antlr.WACCParser.NewScopeContext
import antlr.WACCParser.PrintStatContext
import antlr.WACCParser.PrintlnStatContext
import antlr.WACCParser.ReadStatContext
import antlr.WACCParser.ReturnStatContext
import antlr.WACCParser.SemiColonContext
import antlr.WACCParser.SkipContext
import antlr.WACCParser.StatContext
import antlr.WACCParser.WhileDoContext
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.AnyArrayT
import ic.org.ast.AnyPairTs
import ic.org.ast.ArrayT
import ic.org.ast.Assign
import ic.org.ast.BegEnd
import ic.org.ast.BoolT
import ic.org.ast.CharT
import ic.org.ast.Decl
import ic.org.ast.Exit
import ic.org.ast.For
import ic.org.ast.Free
import ic.org.ast.GlobalScope
import ic.org.ast.Ident
import ic.org.ast.If
import ic.org.ast.IntT
import ic.org.ast.PairT
import ic.org.ast.Print
import ic.org.ast.Println
import ic.org.ast.Read
import ic.org.ast.Return
import ic.org.ast.Scope
import ic.org.ast.Skip
import ic.org.ast.Stat
import ic.org.ast.StatChain
import ic.org.ast.StringT
import ic.org.ast.While
import ic.org.util.ControlFlowTypeError
import ic.org.util.InvalidReturn
import ic.org.util.NOT_REACHED
import ic.org.util.Parsed
import ic.org.util.TypeError
import ic.org.util.combineWith
import ic.org.util.errors
import ic.org.util.flatCombine
import ic.org.util.flatMap
import ic.org.util.startPosition
import ic.org.util.validate
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
    .validate({ it.type is IntT || it.type is StringT || it.type is CharT || (it.type is ArrayT && (it.type as ArrayT).type is CharT) },
      {
        val supported = listOf(IntT, CharT, StringT, ArrayT.make(CharT))
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

  is ForDoContext -> asAst(scp)

  is NewScopeContext -> scp.nested().let { newScope ->
    stat().asAst(newScope).map { BegEnd(it, scp, startPosition) }
  }

  is SemiColonContext -> asAst(scp)

  else -> NOT_REACHED()
}

fun WhileDoContext.asAst(scope: Scope) = scope.nested().let { newScope ->
  expr().asAst(scope)
    .validate(
      { it.type is BoolT },
      { TypeError(startPosition, BoolT, "While condition", it) })
    .combineWith(stat().asAst(newScope)) { cond, body ->
      While(cond, body, scope, startPosition)
    }
}

fun ForDoContext.asAst(scope: Scope): Parsed<For> {
  val newScope = scope.nested()

  val init = stat(0).asAst(scope)
  val incr = stat(1).asAst(scope)
  val body = stat(2).asAst(newScope)

  val cond = expr().asAst(scope)
    .validate({
      it.type is BoolT
    }, {
      TypeError(startPosition, BoolT, "For condition", it)
    })

  return if (cond is Valid && init is Valid && incr is Valid && body is Valid) {
    For(init.a, cond.a, incr.a, body.a, scope, startPosition).valid()
  } else {
    (init.errors + cond.errors + incr.errors + body.errors).invalid()
  }
}

fun IfElseContext.asAst(scope: Scope): Parsed<If> {
  val thenScope = scope.nested()
  val elseScope = scope.nested()
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
