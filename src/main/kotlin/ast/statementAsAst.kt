package ic.org.ast

import antlr.WACCParser.Assign_lhsContext
import antlr.WACCParser.Assign_rhsContext
import antlr.WACCParser.StatContext
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.plus

internal fun StatContext.asAst(scope: Scope): Parsed<Stat> {
  return when {
    SKP() != null -> Skip(scope).valid()
    ASSIGN() != null && assign_lhs() != null -> {
      val lhs = assign_lhs().asAst(scope)
      val rhs = assign_rhs().asAst(scope)

      return if (lhs is Valid && rhs is Valid) {
        Assign(lhs.a, rhs.a, scope).valid()
      } else {
        (lhs.errors + rhs.errors).invalid()
      }
    }
    ASSIGN() != null && assign_lhs() == null -> {
      val type = type().asAst(scope)
      val ident = Ident(ID().text).valid()
      val rhs = assign_rhs().asAst(scope)

      return if (type is Valid && ident is Valid && rhs is Valid) {
        Decl(type.a, ident.a, rhs.a, scope).valid()
      } else {
        (type.errors + rhs.errors).invalid()
      }
    }
    READ() != null -> assign_lhs().asAst(scope).map {
      Read(
        it,
        scope
      )
    }
    FREE() != null -> {
      expr().asAst(scope).flatMap {
        // FREE may only be called in expressions that evaluate to types PairT or ArrayT
        if (it.type is AnyPairTs || it.type is AnyArrayT)
          Free(it, scope).valid()
        else
          TypeError(
            startPosition,
            listOf(
              AnyArrayT(),
              AnyPairTs()
            ),
            it.type,
            "Free"
          )
            .toInvalidParsed()
      }
    }
    RETURN() != null -> {
      return if (scope is GlobalScope) {
        InvalidReturn(RETURN().position).toInvalidParsed()
      } else {
        expr().asAst(scope).map { Return(it, scope) }
      }
    }
    EXIT() != null -> {
      val expr = expr().asAst(scope)
      return if (expr is Valid) {
        // Make sure we return an int
        if (expr.a.type != IntT) {
          TypeError(
            expr().startPosition,
            IntT,
            expr.a.type,
            "exit"
          ).toInvalidParsed()
        } else {
          expr.map { Exit(it, scope) }
        }
      } else {
        expr.errors.invalid()
      }
    }
    PRINT() != null -> expr().asAst(scope).map {
      Print(
        it,
        scope
      )
    }
    PRINTLN() != null -> expr().asAst(scope).map {
      Println(
        it,
        scope
      )
    }
    IF() != null -> {
      val expr = expr().asAst(ControlFlowScope(scope))
      val statTrue = stat(0).asAst(ControlFlowScope(scope))
      val statFalse = stat(1).asAst(ControlFlowScope(scope))

      return if (expr is Valid && statTrue is Valid && statFalse is Valid) {
        when {
          expr.a.type != BoolT -> TypeError(
            startPosition,
            BoolT,
            expr.a.type,
            IF().text
          ).toInvalidParsed()
          else -> TODO("Need to check return types of statTrue and statFalse if they have a return")
          //          else -> If(expr.a, statTrue.a, statFalse.a, scope).valid()
        }
      } else {
        (expr.errors + statTrue.errors + statFalse.errors).invalid()
      }
    }
    WHILE() != null -> {
      assert(stat().size == 1)
      val newScope = ControlFlowScope(scope)
      val e = expr().asAst(scope)
      val s = stat()[0].asAst(newScope)

      return when {
        e !is Valid || s !is Valid ->
          (e.errors + s.errors).invalid()
        e.a.type != BoolT ->
          TypeError(
            WHILE().position,
            BoolT,
            e.a.type,
            "While condition"
          ).toInvalidParsed()
        else ->
          While(e.a, s.a, newScope).valid()
      }
    }

    BEGIN() != null && END() != null -> {
      // Should only have one stat
      assert(stat().size == 1)
      val newScope = ControlFlowScope(scope)
      return stat()[0].asAst(newScope).map {
        BegEnd(
          it,
          newScope
        )
      }
    }
    SEMICOLON() != null -> {
      // In a stat chain, we should only have two statements
      assert(stat().size == 2)
      // Make sure the two statements are valid
      val stat1 = stat()[0].asAst(scope)
      val stat2 = stat()[1].asAst(scope)

      return when {
        stat1 !is Valid || stat2 !is Valid ->
          (stat1.errors + stat2.errors).invalid()
        stat1.a is Return ->
          // TODO this might break with an NPE because of revisiting nodes
          ControlFlowTypeError(stat()[1].startPosition, stat()[1].text).toInvalidParsed()
        else -> StatChain(stat1.a, stat2.a, scope).valid()
      }
    }
    else -> TODO()
  }
}

private fun Assign_lhsContext.asAst(scope: Scope): Parsed<AssLHS> {
  TODO("not implemented")
}

private fun Assign_rhsContext.asAst(scope: Scope): Parsed<AssRHS> {
  TODO("not implemented")
}