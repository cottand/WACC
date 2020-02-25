package ic.org.ast.build

import antlr.WACCParser.*
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.ast.*
import ic.org.util.*
import kotlinx.collections.immutable.plus

private val escapeCharMap = mapOf(
  '0' to 0.toChar(),
  'f' to 12.toChar(),
  'b' to '\b',
  't' to '\t',
  'n' to '\n',
  'r' to '\r'
)

internal fun ExprContext.asAst(scope: Scope): Parsed<Expr> = when (this) {
  // Parse Int literal but check it is a valid size
  is IntLitExprContext -> when (val i = int_lit().text.toLong()) {
    in IntLit.range -> IntLit(i.toInt()).valid()
    else -> IntegerOverflowError(startPosition, i).toInvalidParsed()
  }

  is BoolLitExprContext -> BoolLit(BOOL_LIT().text!!.toBoolean()).valid()
  is CharLitExprContext -> {
    val txt = CHAR_LIT().text.toCharArray()
    if (txt.contentEquals("\'\\0\'".toCharArray()))
      CharLit(0.toChar()).valid()
    else
      CharLit(if (txt[1] == '\\') escapeCharMap[txt[2]] ?: txt[1] else txt[1]).valid()
  }
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

  is BinOpContext -> flatCombine(expr(0).asAst(scope), expr(1).asAst(scope)) { e1, e2 ->
    val binOp = extractBinOp()
    BinaryOperExpr.build(e1, binOp, e2, startPosition)
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
      ArrayElemExpr.make(startPosition, it, exprs.valids, scope)
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
