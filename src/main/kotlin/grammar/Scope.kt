package ic.org.grammar

import arrow.core.Option
import arrow.core.or
import arrow.core.toOption
import arrow.core.valid
import ic.org.Parsed
import ic.org.Position
import ic.org.RedeclarationError

/**
 * Represents a WACC Scope.
 *
 * It is stateful (!!) in that it holds a [MutableList], [variables], which contains all the
 * [Variable]s of this [Scope]
 */
sealed class Scope {
  /**
   * Returns the [Variable] associated with [ident]. If [ident] exists twice in this scope and/or
   * one if the parent scopes, the lowest level [Variable] is returned.
   *
   * So if several [Variable]s with the same [Ident] exist in this scope and of this scope's
   * parent's, this scope's will be returned.
   */
  abstract fun getVar(ident: Ident): Option<Variable>

  fun getVar(identName: String) = getVar(Ident(identName))

  // Some syntactic sugar
  operator fun get(ident: Ident) = getVar(ident)

  /**
   * Get the [Variable] corresponding to this [identName] if found.
   */
  operator fun get(identName: String) = getVar(identName)

  /**
   * Stateful list that holds all the [Variable]s defined in this scope only (not its parents').
   */
  internal val variables = HashMap<Ident, Variable>()

  /**
   * Adds [variable] to the current [Scope]. Used in variable delcaration and function parameter
   * creation.
   */
  fun addVariable(pos: Position, variable: Variable): Parsed<Variable> =
    if (variables.put(variable.ident, variable) == null)
      variable.valid()
    else
      RedeclarationError(pos, variable.ident).toInvalidParsed()

  val globalFuncs: Map<String, FuncIdent>
    get() = when (this) {
      is GlobalScope -> functions
      is FuncScope -> globalScope.functions
      is ControlFlowScope -> parent.globalFuncs
    }
}

/**
 * Global scope. Unique and per program, parent of all [ControlFlowScope]s outside of [FuncScope]s.
 */
class GlobalScope : Scope() {
  override fun getVar(ident: Ident): Option<Variable> = variables[ident].toOption()
  internal val functions = HashMap<String, FuncIdent>()

  fun addFunction(pos: Position, f: FuncIdent) =
    if (functions.put(f.name.name, f) == null)
      f.valid()
    else
      RedeclarationError(pos, f.name).toInvalidParsed()
}

/**
 * Scope created by [funcIdent]. Does not see [GlobalScope] variables, and is parent of
 * [ControlFlowScope] defined inside the [Func].
 */
data class FuncScope(val funcIdent: Ident, val globalScope: GlobalScope) : Scope() {

  // A [FuncScope] does not have any parent scopes, so if the variable is not here, return an
  // option.
  override fun getVar(ident: Ident): Option<Variable> = variables[ident].toOption()
}

/**
 * Scope created inside another [Scope] (could be any) that references its [parent].
 *
 * Created by [BegEnd] or by constructs like [If] and [While] inside another [Scope] (the [parent])
 *
 */
data class ControlFlowScope(val parent: Scope) : Scope() {

  // Return whatever ident is found in this scope's varMap, and look in its parent's otherwise.
  override fun getVar(ident: Ident): Option<Variable> =
    variables[ident].toOption() or parent.getVar(ident)
}

// TODO Change all scope accesses to fit with these new definitions of Variable:
sealed class Variable {
  abstract val type: Type
  abstract val ident: Ident
  // abstract val value: Nothing // TODO revisit at backend
}

data class DeclVariable(
  override val type: Type,
  override val ident: Ident,
  val rhs: AssRHS
) : Variable()

data class ParamVariable(override val type: Type, override val ident: Ident) : Variable() {
  constructor(param: Param) : this(param.type, param.ident)
}

data class FuncIdent(val retType: Type, val name: Ident, val params: List<Param>)
