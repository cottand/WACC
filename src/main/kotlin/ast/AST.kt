package ic.org.ast

import antlr.WACCParser
import antlr.WACCParser.Array_elemContext
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.*
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

internal fun WACCParser.TypeContext.asAst(scope: Scope): Parsed<Type> {
  TODO("not implemented")
}

fun WACCParser.ProgContext.asAst(scope: Scope): Parsed<Prog> {
  val funcs = func().map { it.asAst(ControlFlowScope(scope)) }
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

internal fun WACCParser.ExprContext.asAst(scope: Scope): Parsed<Expr> =
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


