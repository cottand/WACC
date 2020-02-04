package ic.org.graph

import arrow.core.*
import ic.org.*
import ic.org.grammar.*
import kotlinx.collections.immutable.plus

/**
 * A WACC program control flow (inside a scope) may be represented as a Graph. [Node]
 * represents a single node in that graph, which may have different shapes depending on the
 * branching of the program.
 *
 * A [Node]'s outgoing edges are represented as pointers to the next
 * nodes.
 *
 * A leaf in the graph would correspond to a [Return] or an [Exit]. We represent those as
 * [LeafReturn]s
 *
 * A branch may not return. In that case, we need to backtrack to the nearest '`;`' and check the
 * return type of the code that follows after the branching out first happened. For example,
 * branches are represented as [IfElseNode]s, where [IfElseNode.next] is the code that follows.
 *
 * To generate [Node]s from [Stat]s, see [Stat.asGraph]
 */
sealed class Node {
  abstract val returnType: Parsed<Option<Type>>

  fun checkReturnType(expectedType: Type, ident: Ident): Parsed<None> =
    returnType.flatMap { maybeType ->
      maybeType.fold({
        ControlFlowTypeError(expectedType).toInvalidParsed()
      }, { actualType ->
        if (!actualType.matches(expectedType))
          IllegalFunctionReturnTypeError(ident, expectedType, actualType).toInvalidParsed()
        else None.valid()
      })
    }

  /**
   * Utility functions for type checking in the control flow branches
   */
  companion object {
    /**
     * Checks whether a code block [nextStatement] returns the same as [bodyType].
     * If yes, then we return a succesful [Parsed] of the returning [Type], and otherwise we let
     * [checkTypeDiscrepancies] return an invalid [Parsed]
     *
     * If [nextStatement] returns nothing, we return a [ControlFlowTypeError]
     */
    fun checkBodyWithNextStatementType(bodyType: Type, nextStatement: Node): Parsed<Option<Type>> {
      val nextReturn = nextStatement.returnType
      return if (nextReturn !is Valid)
        nextReturn.errors.invalid()
      else
        nextReturn.a.fold({
          ControlFlowTypeError(bodyType).toInvalidParsed()
        }, { nextType ->
          checkTypeDiscrepancies(bodyType, nextType)
        })
    }

    /**
     * Checks whether [t1] and [t2] return the same type, and returns an invalid [Parsed] if they
     * do not.
     */
    fun checkTypeDiscrepancies(t1: Type, t2: Type): Parsed<Option<Type>> =
      if (t1.matches(t2)) t1.toOption().valid()
      else ControlFlowTypeError(t1, t2).toInvalidParsed()
  }
}

/**
 * Node that represents an [If] followed by more code, [next].
 */
class IfElseNode(val thenBody: Node, val elseBody: Node, val next: Node) : Node() {

  /**
   * This is the most complex case. An [If] followed by code may return in both its branches,
   * none, or either.
   */
  override val returnType: Parsed<Option<Type>> by lazy {
    val thenReturn = thenBody.returnType
    val elseReturn = elseBody.returnType
    when {
      thenReturn !is Valid || elseReturn !is Valid ->
        (thenReturn.errors + elseReturn.errors).invalid()

      // Both branches reutrn
      thenReturn.a is Some && elseReturn.a is Some -> {
        val (thenType, elseType) = (thenReturn.a as Some).t to (elseReturn.a as Some).t
        checkTypeDiscrepancies(thenType, elseType)
      }
      // The Else branch returns something, but not the Then branch
      thenReturn.a is None && elseReturn.a is Some ->
        checkBodyWithNextStatementType((elseReturn.a as Some<Type>).t, next)

      // The Then branch returns something, but not the Else branch
      thenReturn.a is Some && elseReturn.a is None ->
        checkBodyWithNextStatementType((thenReturn.a as Some<Type>).t, next)

      // No branch returns, so this code block returns whatever the code that follows returns.
      else -> next.returnType
      // else -> Option.empty<Type>().valid(
    }
  }
}

/**
 * Represents a new Scope followed by a statement [enxt]
 */
class BegEndNode(val scopeBody: Node, val next: Node) : Node() {
  override val returnType by lazy {
    scopeBody.returnType.flatMap {
      it.fold({
        next.returnType
      }, {
        // A new Scope should not contain Return statements if it is followd by a statement.
        ControlFlowTypeError("Unreachable statement").toInvalidParsed()
      })
    }
  }
}

class TerminalBegEndNode(private val scopeBody: Node) : Node() {
  override val returnType by lazy { scopeBody.returnType }
}

/**
 * Represents an [If] that is `not` follows by more code. Therefore, we may not fall back on that
 * if one of the branches does not return.
 */
class TerminalIfElseNode(val thenBody: Node, val elseBody: Node) : Node() {
  override val returnType: Parsed<Option<Type>> by lazy {
    val thenReturn = thenBody.returnType
    val elseReturn = elseBody.returnType
    when {
      thenReturn !is Valid || elseReturn !is Valid ->
        (thenReturn.errors + elseReturn.errors).invalid()

      // An IfElse at the end of a flow were both branches return
      thenReturn.a is Some && elseReturn.a is Some -> {
        val (thenType, elseType) = (thenReturn.a as Some).t to (elseReturn.a as Some).t
        checkTypeDiscrepancies(thenType, elseType)
      }

      // An IfElse at the end of a flow without both branches returning
      else -> Option.empty<Type>().valid()
    }
  }
}

/**
 * Represents an [Exit] or a [Return] statements. Ie, Leaves of the Graph.
 */
class LeafReturn(val type: Type) : Node() {
  /**
   * The type of a [LeafReturn] is [Return.expr]'s type, or the expected type if this is an
   * [Exit] statement. See [Stat.asGraph]
   */
  override val returnType: Parsed<Option<Type>> = type.toOption().valid()
}

/**
 * Represents a branch that does not return. Upon hitting this branch, [returnType] would
 * backtrack into, for example, [IfElseNode.next]
 */
object LeafExit : Node() {
  /**
   * A non-returning branch returns nothing, ie, a [None], also a [Option.empty]
   */
  override val returnType: Parsed<Option<Type>> = Option.empty<Type>().valid()
}

fun Stat.asGraph(expectedType: Type): Node {
  fun Stat.asGraphHelper(): Node = when (this) {
    is StatChain ->
      when (thisStat) {
        is If -> IfElseNode(
          thenBody = thisStat.then.asGraphHelper(),
          elseBody = thisStat.`else`.asGraphHelper(),
          next = nextStat.asGraphHelper()
        )
        is BegEnd -> BegEndNode(
          scopeBody = thisStat.stat.asGraphHelper(),
          next = nextStat.asGraphHelper()
        )
        is Return, is Exit ->
          // Checked at [asAst()] generation
          throw IllegalStateException("Stat chain should never come after an exit!(")
        else -> nextStat.asGraphHelper()
      }

    is Return -> LeafReturn(expr.type)
    // We tolerate an exit statement where a Return would be expected
    is Exit -> LeafReturn(expectedType)

    is If -> TerminalIfElseNode(
      thenBody = then.asGraphHelper(),
      elseBody = `else`.asGraphHelper()
    )

    is BegEnd -> TerminalBegEndNode(stat.asGraphHelper())
    else -> LeafExit
  }
  return asGraphHelper().print()
}
