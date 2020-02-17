@file:Suppress("LeakingThis")

package ic.org.ast

import arrow.core.*
import ic.org.arm.*
import ic.org.util.Position
import ic.org.util.RedeclarationError

/**
 * Represents a WACC Scope.
 *
 * It is stateful (!!) in that it holds a [MutableList], [variables], which contains all the
 * [Variable]s of this [Scope]
 */
sealed class Scope {

  /**
   * Stateful list that holds all the [Variable]s defined in this scope only (not its parents').
   */
  internal val variables = LinkedHashMap<Ident, Variable>()

  /**
   * How big the stack should be grown down upon starting a new stack. Determined by the variables that live on
   * this scope's stack. Value relative to the address of the parent's scope stack.
   * Grows as stuff is added to the stack!
   */
  fun stackSizeSoFar() = variables.toList().fold(0) { stack, (_, v) -> stack + v.type.size.bytes }

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
   * Adds a variable to the current [Scope]. Used in variable delcaration and function parameter
   * creation.
   *
   * @see Scope.stackSizeSoFar
   */
  fun addVariable(pos: Position, t: Type, i: Ident) =
    Variable(t, i, scope = this, addrFromSP = stackSizeSoFar() + t.size.bytes).let {
      if (variables.put(it.ident, it) == null)
        it.valid()
      else
        RedeclarationError(pos, it.ident).toInvalidParsed()
    }

  /**
   * Overloaded [addVariable] by using [param]
   */
  fun addParameter(pos: Position, param: Param) = addVariable(pos, param.type, param.ident)

  val globalFuncs: Map<String, FuncIdent>
    get() = when (this) {
      is GlobalScope -> functions
      is FuncScope -> gScope.functions
      is ControlFlowScope -> parent.globalFuncs
    }

  /**
   * Returns the encapsulation of a scope in assembly instructions by growing the stack down to allocate local variables
   *
   * TODO make sure it is used by Prog.instr() (OK), Func.instr(), and BegEnd Stat.
   */
  fun makeInstrScope() = stackSizeSoFar().let { size ->
    SUBInstr(s = false, rd = SP, rn = SP, int8b = size) to
      ADDInstr(s = false, rd = SP, rn = SP, int8b = size)
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
 * Scope created by a function. Does not see [GlobalScope] variables, and is parent of
 * [ControlFlowScope] defined inside the [Func].
 *
 * TODO remove ident which is for debuggin purposes
 */
data class FuncScope(val ident: Ident, val gScope: GlobalScope) : Scope() {

  // A [FuncScope] does not have any parent scopes, so if the variable is not here, return an
  // option.
  override fun getVar(ident: Ident): Option<Variable> = variables.getOption(ident)
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

/**
 * Represents a WACC variable.
 *
 * Primitives and complex structures pointers all live in the stack,
 * whose address is represented by [addrFromSP]. During code generation, [BegEnd], [Call] and [Prog]
 * have the responsibility of growing the stack down before placing stuff on it.
 */
data class Variable(val type: Type, val ident: Ident, val scope: Scope, val addrFromSP: Int)

data class FuncIdent(val retType: Type, val name: Ident, val params: List<Variable>, val funcScope: FuncScope) {
  val label = Label("f_" + name.name)
}
