package ic.org

import antlr.WACCParser
import arrow.core.Validated.*
import arrow.core.invalid
import arrow.core.valid
import ic.org.grammar.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus

fun WACCParser.FuncContext.asAst(scope: Scope): Parsed<Func> {
  println("parsing func")
  val type = this.type().asAst(ControlFlowScope(scope))
  val ident = Ident(this.ID().text).valid() // TODO are there any checks on identifiers needed
  val params = param_list().param().map { it.asAst(ControlFlowScope(scope)) }
  val stat = stat().asAst(ControlFlowScope(scope))

  TODO("Is this func valid? We probably need to make checks on the stat")

  return if (params.areAllValid && ident is Valid && type is Valid && stat is Valid) {
    val validParams = params.map { (it as Valid).a }
    Func(type.a, ident.a, validParams, stat.a).valid()
  } else {
    (type.errors + ident.errors + params.errors + stat.errors).invalid()
  }
}

private fun WACCParser.ParamContext.asAst(scope: Scope): Parsed<Param>  {
  println("parsing param")
  TODO("not implemented")
}

private fun WACCParser.TypeContext.asAst(scope: Scope): Parsed<Type> {
  println("parsing type")
  TODO("not implemented")
}

fun WACCParser.ProgContext.asAst(scope: Scope): Parsed<Prog> {
  println("parsing prog")
  val funcs = func().map { it.asAst(ControlFlowScope(scope)) }
  val antlrStat = stat()
    ?: return persistentListOf(SyntacticError("Malformed program at $text")).invalid()
  val stat = antlrStat.asAst(ControlFlowScope(scope))

  // Check if the return type matches!

  return if (funcs.areAllValid && stat is Valid) {
    val validFuncs = funcs.map { (it as Valid).a }
    Prog(validFuncs, stat.a).valid()
  } else {
    (funcs.errors + stat.errors).invalid()
  }
}

private fun WACCParser.StatContext.asAst(scope: Scope): Parsed<Stat> {
  TODO()
}

private fun WACCParser.Assign_lhsContext.asAst(scope: Scope): Parsed<AssLHS>
{
  TODO()
}
