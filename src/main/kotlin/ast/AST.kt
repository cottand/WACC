package ic.org.ast

import antlr.WACCParser.*
import arrow.core.Validated.Valid
import arrow.core.getOrElse
import arrow.core.invalid
import arrow.core.toOption
import arrow.core.valid
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import java.util.*
import kotlin.collections.HashSet

fun FuncContext.asAst(gScope: GlobalScope): Parsed<Func> {
  val ident = Ident(this.ID().text)
  val funcScope = FuncScope(ident)
  val type = type().asAst()
  // TODO are there any checks on identifiers needed
  if (type !is Valid) return type.errors.invalid()
  val params = param_list().toOption().fold({
    emptyList<Parsed<Param>>()
  }, { list ->
    val set = HashSet<Param>()
    val repeats = LinkedList<Param>()
    val paramList = list.param().map { it.asAst()}
    if (paramList.areAllValid){
      paramList.valids.forEach {
        if (!set.add(it))
          repeats.add(it)
      }
    }

    if (repeats.size > 0)
      repeats.map{DuplicateParamError(startPosition, it.ident).toInvalidParsed()}.toPersistentList()
    else
      paramList
  })
  val stat = stat().asAst(funcScope)

  return if (params.areAllValid && stat is Valid) {
    val validParams = params.map { (it as Valid).a }
    Func(type.a, ident, validParams, stat.a)
      .also { gScope.addFunction(startPosition, it) }
      .valid()
  } else {
    (type.errors + params.errors + stat.errors).invalid()
  }
}

private fun ParamContext.asAst(): Parsed<Param> =
  this.type().asAst()
    .map { Param(it, Ident(ID().text)) }

internal fun TypeContext.asAst(): Parsed<Type> =
  when {
    base_type() != null -> base_type().asAst().valid()
    pair_type() != null -> pair_type().asAst()
    array_type() != null -> array_type().asAst()
    else -> NOT_REACHED()
  }

private fun Base_typeContext.asAst(): BaseT =
  when {
    INT() != null -> IntT
    BOOL() != null -> BoolT
    CHAR() != null -> CharT
    STRING() != null -> StringT
    else -> NOT_REACHED()
  }

private fun Pair_typeContext.asAst(): Parsed<PairT> {
  val fstType = pair_elem_type(0).asAst()
  val sndType = pair_elem_type(1).asAst()
  return if (fstType is Valid && sndType is Valid)
    PairT(fstType.a, sndType.a).valid()
  else
    (fstType.errors + sndType.errors).invalid()
}

// TODO When checking Types: If NDPairT, we need to recurse to find the right Pair Type!
private fun Pair_elem_typeContext.asAst(): Parsed<Type> =
  when {
    base_type() != null -> base_type().asAst().valid()
    array_type() != null -> array_type().asAst()
    PAIR() != null -> NDPairT.valid()
    else -> NOT_REACHED()
  }

private fun Array_typeContext.asAst(): Parsed<ArrayT> {
  fun recurseArrayT(
      arrayT: Array_typeContext,
      currentDepth: Int
  ): Parsed<Pair<Type, Int>> = when {
    arrayT.array_type() != null -> recurseArrayT(array_type(), currentDepth + 1)
    arrayT.base_type() != null -> arrayT.base_type().asAst().valid().map { it to currentDepth }
    arrayT.pair_type() != null -> arrayT.pair_type().asAst().map { it to currentDepth }
    else -> NOT_REACHED()
  }
  return recurseArrayT(this, 1).map { (type, depth) -> ArrayT.make(type, depth) }
}

/**
 * Entry level of recursive AST conversion. Takes a [Scope], [gScope] or creates a [GlobalScope]
 * by default, from which all scopes (except [FuncScope]s] inherit from.
 */
fun ProgContext.asAst(gScope: GlobalScope = GlobalScope()): Parsed<Prog> {
  val funcs = func().map { it.asAst(gScope) }
  val antlrStat = stat()
  // TODO rewrite syntactic error message with this.startPosition
    ?: return persistentListOf(SyntacticError("Malformed program at $text")).invalid()
  val stat = antlrStat.asAst(gScope)

  return if (funcs.areAllValid && stat is Valid)
    Prog(funcs.valids, stat.a).valid()
  else
    (funcs.errors + stat.errors).invalid()
}

internal fun Assign_lhsContext.asAst(scope: Scope): Parsed<AssLHS> =
  when {
    ID() != null -> scope[ID().text].fold({
      UndefinedIdentifier(startPosition, ID().text).toInvalidParsed()
    }, { IdentLHS(it.ident).valid() })

    array_elem() != null -> scope[array_elem().ID().text].fold({
      UndefinedIdentifier(startPosition, array_elem().text).toInvalidParsed()
    }, { variable ->
      val exprs = array_elem().expr().map { it.asAst(scope) }
      if (exprs.areAllValid)
        ArrayElemLHS(ArrayElem(variable.ident, exprs.valids)).valid()
      else
        exprs.errors.invalid()
    })

    // TODO revisit pair_elem().text vs ID().text
    pair_elem() != null -> pair_elem().expr().asAst(scope)
      .validate(
        { it is IdentExpr },
        { TypeError(startPosition, AnyPairTs(), it.type, pair_elem().text) })
      .flatMap { pairIdent ->
        scope[pair_elem().expr().text].fold({
          UndefinedIdentifier(startPosition, pair_elem().text).toInvalidParsed()
        }, {
          if (pair_elem().FST() != null)
            PairElemLHS(Fst(pairIdent))
          else {
            PairElemLHS(Snd(pairIdent))
          }.valid()
        })
      }

    else -> NOT_REACHED()
  }

internal fun Assign_rhsContext.asAst(scope: Scope): Parsed<AssRHS> {
  when {
    array_lit() != null -> {
      val tokExprs = array_lit().expr()
      if (tokExprs.isEmpty()) {
        return ArrayLit(emptyList()).valid()
      }

      // Transform all the expressions to ASTs
      val exprs = tokExprs.map { it.asAst(scope) }
      if (!exprs.areAllValid) {
        return exprs.errors.invalid()
      }
      val valid = exprs.valids

      // Make sure expressions all have the same type
      val t = valid[0].type
      for (e in valid) {
        if (e.type != t) {
          return TypeError(tokExprs[0].startPosition, t, e.type, "array building")
            .toInvalidParsed()
        }
      }

      return ArrayLit(valid).valid()
    }
    NEWPAIR() != null -> {
      assert(expr().size == 2)

      val e1 = expr(0).asAst(scope)
      val e2 = expr(1).asAst(scope)

      return if (e1 is Valid && e2 is Valid) {
        Newpair(e1.a, e2.a).valid()
      } else {
        (e1.errors + e2.errors).invalid()
      }
    }
    pair_elem() != null ->
      return pair_elem().expr().asAst(scope).map {
        if (pair_elem().FST() != null)
          PairElemRHS(Fst(it))
        else
          PairElemRHS(Snd(it))
      }

    CALL() != null -> {
      // Make ASTs out of all the args
      val args: List<Parsed<Expr>> =
        arg_list()
          ?.expr().toOption().getOrElse { emptyList() }
          .map { it.asAst(scope) }
      if (!args.areAllValid) {
        return args.errors.invalid()
      }

      // TODO make sure arg types are good

      val id = ID().text
      return scope[id].fold({
        Call(Ident(id), args.valids).valid()
      }, {
        UndefinedIdentifier(ID().position, id).toInvalidParsed()
      })
    }

    expr() != null -> {
      return expr(0).asAst(scope).map { ExprRHS(it) }
    }
    else -> NOT_REACHED()
  }
}
