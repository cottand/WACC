package ic.org.ast.build

import antlr.WACCParser.*
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.*
import ic.org.util.*
import kotlinx.collections.immutable.plus

internal fun ExprContext.asAst(scope: Scope): Parsed<Expr> = when (this) {
  // Parse Int literal but check it is a valid size
  is IntLitExprContext -> when (val i = int_lit().text.toLong()) {
    in IntLit.range -> IntLit(i.toInt()).valid()
    else -> IntegerOverflowError(startPosition, i).toInvalidParsed()
  }

  is BoolLitExprContext -> BoolLit(BOOL_LIT().text!!.toBoolean()).valid()
  is CharLitExprContext -> CharLit(CHAR_LIT().text.toCharArray()[1]).valid()
  is StrLitExprContext -> StrLit(STRING_LIT().text.drop(1).dropLast(1)).valid()
  is PairLitExprContext -> NullPairLit.valid()
  is IdentExprContext -> scope[ID().text].fold({
    UndefinedIdentifier(ID().position, ID().text).toInvalidParsed()
  }, { variable ->
    IdentExpr(variable, scope).valid()
  })

  is ArrayElemExprContext -> array_elem().asAst(scope)
  is UnOpExprContext -> expr().asAst(scope)
    .flatMap {
      UnaryOperExpr.build(it, unary_op().asAst(), startPosition)
    }

  is NestedExprContext -> expr().asAst(scope)

  is BinOpContext -> {
    val e1 = expr(0).asAst(scope)
    val e2 = expr(1).asAst(scope)
    val binOp = extractBinOp()
    if (e1 is Valid && e2 is Valid)
      BinaryOperExpr.build(e1.a, binOp, e2.a, startPosition)
    else
      (e1.errors + e2.errors).invalid()
  }
  else -> NOT_REACHED()
}

private fun BinOpContext.extractBinOp() = when {
  MUL() != null -> MulBO
  DIV() != null -> DivBO
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

private fun Array_elemContext.asAst(scope: Scope): Parsed<ArrayElemExpr> {
  val id = ID().text
  val exprs = expr().map { it.asAst(scope) }
  return scope[id].fold<Parsed<ArrayElemExpr>>({
    (exprs.errors + UndefinedIdentifier(startPosition, id)).invalid()
  }, {
    if (exprs.areAllValid)
      ArrayElemExpr.make(startPosition, it, exprs.valids)
    else
      exprs.errors.invalid()
  })
}

private fun Unary_opContext.asAst(): UnaryOper =
  when {
    NOT() != null -> NotUO
    MINUS() != null -> MinusUO
    LEN() != null -> LenUO
    ORD() != null -> OrdUO
    CHR() != null -> ChrUO
    else -> NOT_REACHED()
  }
