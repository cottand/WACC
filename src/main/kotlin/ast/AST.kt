package ic.org.ast

import antlr.WACCParser
import antlr.WACCParser.Array_elemContext
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.toOption
import arrow.core.valid
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus

fun WACCParser.FuncContext.asAst(): Parsed<Func> {
  val ident = Ident(this.ID().text)
  val funcScope = FuncScope(ident)
  val type = type().asAst()
  // TODO are there any checks on identifiers needed
  if (type !is Valid) return type.errors.invalid()
  val params = param_list().toOption().fold({
    listOf<Parsed<Param>>()
  }, { list ->
    list.param().map { it.asAst(ControlFlowScope(funcScope)) }
  })
  val stat = stat().asAst(ControlFlowScope(funcScope))

  //TODO("Is this func valid? We probably need to make checks on the stat")

  return if (params.areAllValid && stat is Valid) {
    val validParams = params.map { (it as Valid).a }
    Func(type.a, ident, validParams, stat.a)
      .also { GlobalScope.functions.push(it) }
      .valid()
  } else {
    (type.errors + params.errors + stat.errors).invalid()
  }
}

private fun WACCParser.ParamContext.asAst(scope: Scope): Parsed<Param> {
  TODO("not implemented")
}

private fun WACCParser.TypeContext.asAst(): Parsed<Type> =
  when {
    base_type() != null -> base_type().asAst().valid()
    pair_type() != null -> pair_type().asAst()
    array_type() != null -> array_type().asAst()
    else -> NOT_REACHED()
  }

private fun WACCParser.Base_typeContext.asAst(): BaseT =
  when {
    INT() != null -> IntT
    BOOL() != null -> BoolT
    CHAR() != null -> CharT
    STRING() != null -> StringT
    else -> NOT_REACHED()
  }

private fun WACCParser.Pair_typeContext.asAst(): Parsed<PairT> {
  val fstType = pair_elem_type(0).asAst()
  val sndType = pair_elem_type(1).asAst()
  return if (fstType is Valid && sndType is Valid)
    PairT(fstType.a, sndType.a).valid()
  else
    (fstType.errors + sndType.errors).invalid()
}

// TODO When checking Types: If NDPairT, we need to recurse to find the right Pair Type!
private fun WACCParser.Pair_elem_typeContext.asAst(): Parsed<Type> =
  when {
    base_type() != null -> base_type().asAst().valid()
    array_type() != null -> array_type().asAst()
    PAIR() != null -> NDPairT.valid()
    else -> NOT_REACHED()
  }

private fun WACCParser.Array_typeContext.asAst(): Parsed<ArrayT> {
  fun recurseArrayT(
    arrayT: WACCParser.Array_typeContext,
    currentDepth: Int
  ): Parsed<Pair<Type, Int>> = when {
    base_type() != null -> base_type().asAst().valid().map { it to currentDepth }
    pair_type() != null -> pair_type().asAst().map { it to currentDepth }
    array_type() != null -> recurseArrayT(array_type(), currentDepth + 1)
    else -> NOT_REACHED()
  }
  return recurseArrayT(this, 1).map { (type, depth) -> ArrayT(type, depth) }
}

fun WACCParser.ProgContext.asAst(): Parsed<Prog> {
  val funcs = func().map { it.asAst() }
  val antlrStat = stat()
  // TODO rewrite syntactic error message with this.startPosition
    ?: return persistentListOf(SyntacticError("Malformed program at $text")).invalid()
  val stat = antlrStat.asAst(GlobalScope)

  // TODO Check if the return type matches!

  return if (funcs.areAllValid && stat is Valid)
    Prog(funcs.valids, stat.a).valid()
  else
    (funcs.errors + stat.errors).invalid()
}

private fun WACCParser.StatContext.asAst(scope: Scope): Parsed<Stat> {
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
      val type = type().asAst()
      val ident = Ident(ID().text).valid()
      val rhs = assign_rhs().asAst(scope)

      return if (type is Valid && ident is Valid && rhs is Valid) {
        Decl(type.a, ident.a, rhs.a, scope).valid()
      } else {
        (type.errors + rhs.errors).invalid()
      }
    }
    READ() != null -> assign_lhs().asAst(scope).map { Read(it, scope) }
    FREE() != null -> {
      expr().asAst(scope).flatMap {
        // FREE may only be called in expressions that evaluate to types PairT or ArrayT
        if (it.type is AnyPairTs || it.type is AnyArrayT)
          Free(it, scope).valid()
        else
          TypeError(
            startPosition,
            listOf(AnyArrayT(), AnyPairTs()),
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
    EXIT() != null -> expr().asAst(scope).flatMap { expr ->
      // Make sure we return an int
      if (expr.type != IntT)
        TypeError(
          pos = expr().startPosition,
          expectedT = IntT,
          actual = expr.type,
          op = "exit"
        ).toInvalidParsed()
      else
        Exit(expr, scope).valid()
    }
    PRINT() != null -> expr().asAst(scope).map { Print(it, scope) }
    PRINTLN() != null -> expr().asAst(scope).map { Println(it, scope) }
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
      return stat()[0].asAst(newScope).map { BegEnd(it, newScope) }
    }
    SEMICOLON() != null -> {
      // In a stat chain, we should only have two statements
      assert(stat().size == 2)
      // Make sure the two statements are valid
      val stat1 = stat()[0].asAst(scope)
      val stat2 = stat()[1].asAst(scope)

      return if (stat1 is Valid && stat2 is Valid) {
        StatChain(stat1.a, stat2.a, scope).valid()
      } else {
        (stat1.errors + stat2.errors).invalid()
      }
    }
    else -> NOT_REACHED()
  }
}

private fun WACCParser.Assign_lhsContext.asAst(scope: Scope): Parsed<AssLHS> {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun WACCParser.Assign_rhsContext.asAst(scope: Scope): Parsed<AssRHS> {
  return when {
    array_lit() != null -> TODO()
    NEWPAIR() != null -> TODO()
    pair_elem() != null -> TODO()
    CALL() != null -> TODO()
    expr() != null -> TODO()
    else -> throw IllegalStateException("Should never be reached (invalid assign_rhs)")
  }
}

private fun WACCParser.ExprContext.asAst(scope: Scope): Parsed<Expr> =
  when {
    // TODO why is the int overflow test not passing
    int_lit() != null -> when (val i = int_lit().text.toInt()) {
      in Constatns.intRange -> IntLit(i).valid()
      else -> IntegerOverflowError(startPosition, i).toInvalidParsed().print()
    }
    BOOL_LIT() != null
    -> BoolLit(BOOL_LIT().text!!.toBoolean()).valid()
    CHAR_LIT() != null -> CharLit(CHAR_LIT().text.toCharArray()[0]).valid()
    STRING_LIT() != null -> StrLit(STRING_LIT().text).valid()
    PAIR_LIT() != null -> NullPairLit.valid()

    ID() != null -> scope[ID().text].fold({
      VarNotFoundError(ID().position, ID().text).toInvalidParsed()
    }, { variable ->
      IdentExpr(variable).valid()
    })

    array_elem() != null -> array_elem().asAst(scope)

    unary_op() != null -> expr()[0].asAst(scope)
      .flatMap { UnaryOperExpr.make(it, unary_op().asAst(), startPosition) }

    binary_op() != null -> {
      val e1 = expr()[0].asAst(scope)
      val e2 = expr()[1].asAst(scope)
      val binOp = binary_op().asAst()
      if (e1 is Valid && e2 is Valid)
        BinaryOperExpr.make(e1.a, binOp, e2.a, startPosition)
      else
        (e1.errors + e2.errors).invalid()
    }
    else -> NOT_REACHED()
  }

private fun Array_elemContext.asAst(scope: Scope): Parsed<ArrayElemExpr> {
  val id = ID().text
  val exprs = expr().map { it.asAst(scope) }
  return scope[id].fold({
    (exprs.errors + VarNotFoundError(startPosition, id)).invalid()
      as Parsed<ArrayElemExpr>
  }, {
    if (exprs.areAllValid)
      ArrayElemExpr.make(startPosition, it, exprs.valids)
    else
      exprs.errors.invalid()
  })
}

private fun WACCParser.Unary_opContext.asAst(): UnaryOper =
  when {
    NOT() != null -> NotUO
    MINUS() != null -> MinusUO
    LEN() != null -> LenUO
    ORD() != null -> OrdUO
    CHR() != null -> ChrUO
    else -> NOT_REACHED()
  }

private fun WACCParser.Binary_opContext.asAst(): BinaryOper =
  when {
    MUL() != null -> TimesBO
    DIV() != null -> DivisionBO
    MOD() != null -> ModBO
    PLUS() != null -> PlusBO
    MINUS() != null -> MinusBO
    GRT() != null -> GtBO
    GRT_EQ() != null -> GeqBO
    LESS() != null -> LtBO
    LESS_EQ() != null -> LeqBO
    EQ() != null -> EqBO
    NOT_EQ() != null -> NeqBO
    AND() != null -> AndBO
    OR() != null -> OrBO
    else -> NOT_REACHED()
  }


