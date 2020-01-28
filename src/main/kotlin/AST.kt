package ic.org

import antlr.WACCParser
import antlr.WACCParser.Array_elemContext
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.grammar.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus

fun WACCParser.FuncContext.asAst(scope: Scope): Parsed<Func> {
  val type = this.type().asAst(ControlFlowScope(scope))
  // TODO are there any checks on identifiers needed
  if (type !is Valid) return type.errors.invalid()
  val ident = Ident(this.ID().text).valid()
  val params = param_list().param().map { it.asAst(ControlFlowScope(scope)) }
  val stat = stat().asAst(ControlFlowScope(scope))

  //TODO("Is this func valid? We probably need to make checks on the stat")

  return if (params.areAllValid && ident is Valid && type is Valid && stat is Valid) {
    val validParams = params.map { (it as Valid).a }
    Func(type.a, ident.a, validParams, stat.a).valid()
  } else {
    (type.errors + ident.errors + params.errors + stat.errors).invalid()
  }
}

private fun WACCParser.ParamContext.asAst(scope: Scope): Parsed<Param> {
  TODO("not implemented")
}

private fun WACCParser.TypeContext.asAst(scope: Scope): Parsed<Type> {
  TODO("not implemented")
}

fun WACCParser.ProgContext.asAst(scope: Scope): Parsed<Prog> {
  val funcs = func().map { it.asAst(ControlFlowScope(scope)) }
  val antlrStat = stat()
  // TODO rewrite syntactic error message with this.startPosition
    ?: return persistentListOf(SyntacticError("Malformed program at $text")).invalid()
  val stat = antlrStat.asAst(ControlFlowScope(scope))

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
      val type = type().asAst(scope)
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
          TypeError(startPosition, listOf(AnyArrayT(), AnyPairTs()), it.type, "Free")
            .toInvalidParsed()
      }
    }
    RETURN() != null -> TODO()
    EXIT() != null -> TODO()
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
          TypeError(WHILE().position, BoolT, e.a.type, "While condition").toInvalidParsed()
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
    else -> TODO()
  }
}

private fun WACCParser.Assign_lhsContext.asAst(scope: Scope): Parsed<AssLHS> {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun WACCParser.Assign_rhsContext.asAst(scope: Scope): Parsed<AssRHS> {
  TODO("not implemented")
}

private fun WACCParser.ExprContext.asAst(scope: Scope): Parsed<Expr> =
  when {
    // TODO check there are no cases where toInt() ot toBoolean() would fail
    int_lit() != null -> IntLit(int_lit().text.toInt()).valid()
    BOOL_LIT() != null -> BoolLit(BOOL_LIT().text!!.toBoolean()).valid()
    CHAR_LIT() != null -> CharLit(CHAR_LIT().text.toCharArray()[0]).valid()
    STRING_LIT() != null -> StrLit(STRING_LIT().text).valid()
    PAIR_LIT() != null -> NullPairLit.valid()

    ID() != null -> scope[ID().text].fold({
      VarNotFoundError(ID().position, ID().text).toInvalidParsed()
    }, { variable ->
      IdentExpr(variable).valid()
    })

    array_elem() != null -> array_elem().asAst(scope)

    unary_op() != null -> {
      val e = expr()[0].asAst(scope)
      val unOp = unary_op().asAst()
      if (e is Valid && unOp is Valid)
        UnaryOperExpr.make(e.a, unOp.a, startPosition)
      else
        (e.errors + unOp.errors).invalid()
    }

    binary_op() != null -> {
      val e1 = expr()[0].asAst(scope)
      val e2 = expr()[1].asAst(scope)
      val binOp = binary_op().asAst()
      if (e1 is Valid && binOp is Valid && e2 is Valid)
        BinaryOperExpr.make(e1.a, binOp.a, e2.a, startPosition)
      else
        (e1.errors + binOp.errors + e2.errors).invalid()
    }
    else -> TODO()
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

private fun WACCParser.Unary_opContext.asAst(): Parsed<UnaryOper> =
  when {
    NOT() != null -> NotUO.valid()
    MINUS() != null -> MinusUO.valid()
    LEN() != null -> LenUO.valid()
    ORD() != null -> OrdUO.valid()
    CHR() != null -> ChrUO.valid()
    else -> TODO()
  }

private fun WACCParser.Binary_opContext.asAst(): Parsed<BinaryOper> =
  when {
    MUL() != null -> TimesBO.valid()
    DIV() != null -> DivisionBO.valid()
    MOD() != null -> ModBO.valid()
    PLUS() != null -> PlusBO.valid()
    MINUS() != null -> MinusBO.valid()
    GRT() != null -> GtBO.valid()
    GRT_EQ() != null -> GeqBO.valid()
    LESS() != null -> LtBO.valid()
    LESS_EQ() != null -> LeqBO.valid()
    EQ() != null -> EqBO.valid()
    NOT_EQ() != null -> NeqBO.valid()
    AND() != null -> AndBO.valid()
    OR() != null -> OrBO.valid()
    else -> TODO()
  }


