package ic.org

import antlr.WACCParser
import arrow.core.Validated.*
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

  return if (funcs.areAllValid && stat is Valid) {
    val validFuncs = funcs.map { (it as Valid).a }
    Prog(validFuncs, stat.a).valid()
  } else {
    (funcs.errors + stat.errors).invalid()
  }
}

private fun WACCParser.StatContext.asAst(scope: Scope): Parsed<Stat> {
  return when{
    SKP() != null -> Skip(scope).valid()
    ASSIGN() != null -> TODO()
    READ() != null -> {
      val lhs = assign_lhs().asAst(ControlFlowScope(scope))
      return if (lhs is Valid) {
        Read(lhs.a, scope).valid()
      } else {
        lhs.errors.invalid()
      }
    }
    FREE() != null -> {
      val expr = expr().asAst(ControlFlowScope(scope))
      return if (expr is Valid) {
        Free(expr.a, scope).valid()
      } else {
        expr.errors.invalid()
      }
    }
    RETURN() != null -> TODO()
    EXIT() != null -> TODO()
    PRINT() != null -> TODO()
    PRINTLN() != null -> TODO()
    IF() != null -> TODO()
    WHILE() != null -> TODO()
    BEGIN() != null && END() != null -> TODO()
    SEMICOLON() != null -> TODO()
    else -> TODO()
  }
}

private fun WACCParser.Assign_lhsContext.asAst(scope: Scope): Parsed<AssLHS> {
  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun WACCParser.ExprContext.asAst(scope: Scope): Parsed<Expr> =
  when {
    // TODO check there are no cases where toInt() ot toBoolean() would fail
    int_lit() != null -> IntLit(int_lit().text.toInt()).valid()
    BOOL_LIT() != null -> BoolLit(BOOL_LIT().text!!.toBoolean()).valid()
    CHAR_LIT() != null -> CharLit(CHAR_LIT().text.toCharArray()[0]).valid()
    STRING_LIT() != null -> StrLit(STRING_LIT().text).valid()
    PAIR_LIT() != null -> NullPairLit.valid()

    ID() != null ->
      scope[ID().text].fold({
        VarNotFoundError(ID().position, ID().text).toInvalidParsed()
      }, { variable ->
        IdentExpr(variable).valid()
      })
    // TODO check if calling array_elem() twice is possibly a bad idea cos we are getting a child
    //  twice

    array_elem() != null -> {
      val id = array_elem().ID().text
      val exprs = array_elem().expr().map { it.asAst(scope) }
      scope[id].fold({
        (exprs.errors + VarNotFoundError(startPosition, id)).invalid()
          as Parsed<Expr>
      }, {
        if (exprs.areAllValid)
          ArrayElemExpr.make(startPosition, it.declaringStat.id, exprs.valids, scope)
        else
          exprs.errors.invalid()
      })
    }

    unary_op() != null -> UnaryOperExpr(TODO(), TODO()).valid()
    binary_op() != null -> BinaryOperExpr(TODO(), TODO(), TODO()).valid()
    binary_op() != null -> {
      val e1 = expr()[0].asAst(scope)
      val e2 = expr()[1].asAst(scope)
      val binOp = binary_op().asAst()
      when {
          e1 is Valid && binOp is Valid && e2 is Valid-> BinaryOperExpr.make(e1.a, binOp.a, e2.a, startPosition)
        else -> (e1.errors + e2.errors).invalid()
      }
    }

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


