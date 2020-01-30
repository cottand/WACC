package ic.org.grammar

import arrow.core.Option
import arrow.core.or
import arrow.core.toOption
import java.util.LinkedList

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

  operator fun get(identName: String) = getVar(identName)

  /**
   * Stateful list that holds all the [Variable]s defined in this scope only (not its parents').
   */
  val variables = LinkedList<Variable>()

  val functions = LinkedList<Func>()

  internal val varMap
    get() = variables.map { it.declaringStat.id to it }.toMap()
}

/**
 * Global scope. Unique and per program, parent of all [ControlFlowScope]s outside of [FuncScope]s.
 */
object GlobalScope : Scope() {
  override fun getVar(ident: Ident): Option<Variable> = varMap[ident].toOption()
}

/**
 * Scope created by [funcIdent]. Does not see [GlobalScope] variables, and is parent of
 * [ControlFlowScope] defined inside the [Func].
 */
data class FuncScope(val funcIdent: Ident) : Scope() {

  // A [FuncScope] does not have any parent scopes, so if the variable is not here, return an
  // option.
  override fun getVar(ident: Ident): Option<Variable> = varMap[ident].toOption()
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
    varMap[ident].toOption() or parent.getVar(ident)
}

data class Variable(val declaringStat: Decl, val value: Nothing) { // TODO revisit at backend
  val type = declaringStat.type
}

