package ic.org.graph

import arrow.core.*
import ic.org.ControlFlowTypeError
import ic.org.Parsed
import ic.org.errors
import ic.org.grammar.*
import kotlinx.collections.immutable.plus

/**
 * Graph of statements. Used to analyse control flow in the WACC language
 */
class Graph {

}

sealed class Node {
  fun findReturn(): Parsed<Option<Type>> = when (this) {
    is LeafExit ->
      Option.empty<Type>().valid()
    is LeafReturn ->
      type.toOption().valid()
    is StatementNode ->
      next.findReturn()
    is IfElseNode -> {
      val thenReturn = thenBody.findReturn()
      val elseReturn = elseBody.findReturn()
      when {
        thenReturn !is Valid || elseReturn !is Valid ->
          (thenReturn.errors + elseReturn.errors).invalid()
        thenReturn.a is Some && elseReturn.a is Some -> {
          val (thenType, elseType) = (thenReturn.a as Some).t to (elseReturn.a as Some).t
          checkTypeDiscrepancies(thenType, elseType)
        }
        thenReturn.a !is Some && elseReturn.a is Some -> {
          val elseReturnType = (elseReturn.a as Some).t
          checkBodyWithNextStatementType(elseReturnType, next)
        }
        thenReturn.a is Some && elseReturn.a !is Some -> {
          val thenReturnType = (thenReturn.a as Some).t
          checkBodyWithNextStatementType(thenReturnType, next)
        }
        else -> Option.empty<Type>().valid()
      }
    }
    is TerminalIfElseNode -> TODO()
    is BegEndNode -> TODO()
  }
}

fun checkBodyWithNextStatementType(bodyType: Type, nextStatement: Node): Parsed<Option<Type>> {
  val nextReturn = nextStatement.findReturn()
  return if (nextReturn !is Valid)
    nextReturn.errors.invalid()
  else
    nextReturn.a.fold({
      ControlFlowTypeError(bodyType).toInvalidParsed()
    }, { nextType ->
      checkTypeDiscrepancies(bodyType, nextType)
    })
}

fun checkTypeDiscrepancies(t1: Type, t2: Type): Parsed<Option<Type>> =
  if (t1 == t2) t1.toOption().valid()
  else ControlFlowTypeError(t1, t2).toInvalidParsed()

class StatementNode(val next: Node) : Node()
class IfElseNode(val thenBody: Node, val elseBody: Node, val next: Node) : Node()
class BegEndNode(val scopeBody: Node, val next: Node) : Node()
class TerminalIfElseNode(val thenBody: Node, val elseBody: Node) : Node()
class LeafReturn(val type: Type) : Node()
object LeafExit : Node()

data class UnexpectedExit(val stat: Stat) : Exception("$stat")

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
          throw IllegalStateException("Stat chain should never come followed by an exit")
        is While -> TODO()
        else -> nextStat.asGraphHelper()
      }

    is Return -> LeafReturn(expr.type)
    // We tolerate an exit statement where a Return would be expected
    is Exit -> LeafReturn(expectedType)

    is If -> TerminalIfElseNode(
      thenBody = then.asGraphHelper(),
      elseBody = `else`.asGraphHelper()
    )

    is While -> TODO()
    is BegEnd -> TODO()
    else -> LeafExit
  }
  return asGraphHelper()
}