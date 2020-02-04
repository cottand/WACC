package ic.org.ast

import antlr.WACCParser.*
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.valid
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus



/**
 * Entry level of recursive AST conversion. Takes a [Scope], [gScope] or creates a [GlobalScope]
 * by default, from which all scopes (except [FuncScope]s] inherit from.
 */
fun ProgContext.asAst(gScope: GlobalScope = GlobalScope()): Parsed<Prog> {

  val funcs = func().map {
    val t = it.type().asAst()
    val ident = Ident(it.ID().text)
    val params = it.paramsAsAst()
    val funcId = FuncIdent(t, ident, params.valids)
    gScope.addFunction(startPosition, funcId)
    it to params
  }.map { (ctx, ps) ->
    ctx.asAst(gScope, ps)
  }
  val antlrStat = stat()
  // TODO rewrite syntactic error message with this.startPosition
    ?: return persistentListOf(SyntacticError("Malformed program at $text")).invalid()
  val stat = antlrStat.asAst(gScope)

  return if (funcs.areAllValid && stat is Valid)
    Prog(funcs.valids, stat.a).valid()
  else
    (funcs.errors + stat.errors).invalid()
}


fun FuncContext.paramsAsAst(): List<Parsed<Param>> {
  val antlrParams = param_list()?.param() ?: emptyList()
  val counts = HashMap<Param, Int>()
  return antlrParams.map { it.asAst() to it }
    .onEach { (parsed, _) -> counts[parsed] = (counts[parsed] ?: 0) + 1 }
    .map { (parsed, ctx) ->
      parsed.valid().validate({ counts[it] == 1 },
        { DuplicateParamError(ctx.startPosition, it.ident) })
    }
}

fun FuncContext.asAst(gScope: GlobalScope, params: List<Parsed<Param>>): Parsed<Func> {
  val ident = Ident(this.ID().text)
  val funcScope = FuncScope(ident, gScope)
  // TODO we need to infer pair return types possibly
  val antlrParams = param_list()?.param() ?: emptyList()

  val type = type().asAst()
  // TODO are there any checks on identifiers needed

  params.valids.forEachIndexed { i, p ->
    funcScope.addVariable(antlrParams[i].startPosition, ParamVariable(p))
  }

  val stat = stat().asAst(funcScope)
  return if (params.areAllValid && stat is Valid)
    Func(type, ident, params.valids, stat.a).valid()
  else
    (params.errors + stat.errors).invalid()
}

private fun ParamContext.asAst(): Param =
  Param(type().asAst(), Ident(ID().text))

internal fun Pair_elem_typeContext.asAst(): Type =
  when (this) {
    is BaseTPairElemContext -> base_type().asAst()
    is ArrayPairElemContext -> array_type().asAst()
    // Pair type not defined yet, it must be inferred by the caller
    else -> AnyPairTs()
    //is PairPairElemContext -> NDPairT.valid()
    //else -> NOT_REACHED()
  }
