package ic.org.ast.build

import antlr.WACCParser.ArrayElemExprContext
import antlr.WACCParser.Array_elemContext
import antlr.WACCParser.BinOpContext
import antlr.WACCParser.BoolLitExprContext
import antlr.WACCParser.CharLitExprContext
import antlr.WACCParser.ExprContext
import antlr.WACCParser.IdentExprContext
import antlr.WACCParser.IntLitExprContext
import antlr.WACCParser.NestedExprContext
import antlr.WACCParser.PairLitExprContext
import antlr.WACCParser.StrLitExprContext
import antlr.WACCParser.UnOpExprContext
import antlr.WACCParser.Unary_opContext
import arrow.core.invalid
import arrow.core.valid
import ic.org.arm.Ranges
import ic.org.ast.AndBO
import ic.org.ast.ArrayElemExpr
import ic.org.ast.BinaryOperExpr
import ic.org.ast.BoolLit
import ic.org.ast.CharLit
import ic.org.ast.ChrUO
import ic.org.ast.DivBO
import ic.org.ast.EqBO
import ic.org.ast.Expr
import ic.org.ast.GeqBO
import ic.org.ast.GtBO
import ic.org.ast.IdentExpr
import ic.org.ast.IntLit
import ic.org.ast.LenUO
import ic.org.ast.LeqBO
import ic.org.ast.LtBO
import ic.org.ast.MinusBO
import ic.org.ast.MinusUO
import ic.org.ast.ModBO
import ic.org.ast.MulBO
import ic.org.ast.NeqBO
import ic.org.ast.NotUO
import ic.org.ast.NullPairLit
import ic.org.ast.OrBO
import ic.org.ast.OrdUO
import ic.org.ast.PlusBO
import ic.org.ast.Scope
import ic.org.ast.StrLit
import ic.org.ast.UnaryOper
import ic.org.ast.UnaryOperExpr
import ic.org.util.IntegerOverflowError
import ic.org.util.NOT_REACHED
import ic.org.util.Parsed
import ic.org.util.UndefinedIdentifier
import ic.org.util.areAllValid
import ic.org.util.errors
import ic.org.util.flatCombine
import ic.org.util.flatMap
import ic.org.util.position
import ic.org.util.startPosition
import ic.org.util.valids
import kotlinx.collections.immutable.plus

private val escapeCharMap = mapOf(
  "\\0" to 0.toChar(),
  "\\f" to 12.toChar(),
  "\\b" to '\b',
  "\\t" to '\t',
  "\\n" to '\n',
  "\\r" to '\r',
  "\\\"" to '\"',
  "\\\'" to '\'',
  "\\\\" to '\\'
)

internal fun ExprContext.asAst(scope: Scope): Parsed<Expr> = when (this) {
  // Parse Int literal but check it is a valid size
  is IntLitExprContext -> when (val i = int_lit().text.toLong()) {
    in Ranges._32b -> IntLit(i.toInt()).valid()
    else -> IntegerOverflowError(startPosition, i).toInvalidParsed()
  }

  is BoolLitExprContext -> BoolLit(BOOL_LIT().text!!.toBoolean()).valid()
  is CharLitExprContext -> {
    val txt = CHAR_LIT().text.drop(1).dropLast(1)
    CharLit(escapeCharMap[txt] ?: txt[0]).valid()
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
