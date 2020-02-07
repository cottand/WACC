package ic.org.listeners

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeListener
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * Useless [ParseTreeListener] to be passed to the tree walker in order to syntactically check
 * the whole program in a first pass before doing semantic chcks.
 */
class DummyListener : ParseTreeListener {
  override fun enterEveryRule(ctx: ParserRuleContext?) {}

  override fun exitEveryRule(ctx: ParserRuleContext?) {}

  override fun visitErrorNode(node: ErrorNode?) {}

  override fun visitTerminal(node: TerminalNode?) {}
}
