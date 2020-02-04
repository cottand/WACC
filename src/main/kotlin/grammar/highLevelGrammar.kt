package ic.org.grammar

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.antlr.v4.runtime.tree.TerminalNode

// <program>
data class Prog(
    val funcs: List<Func>,
    val firstStat: Stat
)

// <func>
data class Func(
    val retType: Type,
    val ident: Ident,
    val params: List<Param>,
    val stat: Stat
)

// <param>
data class Param(val type: Type, val ident: Ident)

// <stat>
sealed class Stat {
    abstract val scope: Scope
}

data class Skip(override val scope: Scope) : Stat()
data class Decl(val variable: Variable, val rhs: AssRHS, override val scope: Scope) :
    Stat() {
    val type = variable.type
    val ident = variable.ident
}

data class Assign(val lhs: AssLHS, val rhs: AssRHS, override val scope: Scope) : Stat()
data class Read(val lhs: AssLHS, override val scope: Scope) : Stat()
data class Free(val expr: Expr, override val scope: Scope) : Stat()
data class Return(val expr: Expr, override val scope: Scope) : Stat()
data class Exit(val expr: Expr, override val scope: Scope) : Stat()
data class Print(val expr: Expr, override val scope: Scope) : Stat()
data class Println(val expr: Expr, override val scope: Scope) : Stat()
data class If(val cond: Expr, val then: Stat, val `else`: Stat, override val scope: Scope) : Stat()
data class While(val cond: Expr, val stat: Stat, override val scope: Scope) : Stat()
data class BegEnd(val stat: Stat, override val scope: Scope) : Stat()
data class StatChain(val stat1: Stat, val stat2: Stat, override val scope: Scope) : Stat() {
    val thisStat = stat1
    val nextStat = stat2
}

// <assign-lhs>
sealed class AssLHS {
    abstract val type: Type
}

data class IdentLHS(val variable: Variable) : AssLHS() {
    override val type = variable.type
}

data class ArrayElemLHS(val indices: List<Expr>, val variable: Variable) : AssLHS() {
    val ident = variable.ident
    override val type: Type
        get() {
            val arrT = variable.type as ArrayT
            return arrT.nthNestedType(indices.size)
        }
}

data class PairElemLHS(val pairElem: PairElem, val variable: Variable, val pairs: PairT) :
    AssLHS() {
    override val type = when (pairElem) {
        is Fst -> pairs.fstT
        is Snd -> pairs.sndT
    }
}

// <assign-rhs>
sealed class AssRHS {
    // abstract fun fetchType(scope: Scope): Option<Type>
    abstract val type: Type
}

data class ExprRHS(val expr: Expr) : AssRHS() {
    override val type = expr.type
}

/**
 * If the array literal is empty, [contentType] is [EmptyArrayT]
 * Caller should verify content.
 */
data class ArrayLit(val exprs: List<Expr>, val arrT: AnyArrayT) : AssRHS() {
    override val type = arrT
}

data class Newpair(val expr1: Expr, val expr2: Expr) : AssRHS() {
    override val type = PairT(expr1.type, expr2.type)
}

data class PairElemRHS(val pairElem: PairElem, val pairs: PairT) : AssRHS() {
    override val type = when (pairElem) {
        is Fst -> pairs.fstT
        is Snd -> pairs.sndT
    }
}

data class Call(val func: FuncIdent, val args: List<Expr>) : AssRHS() {
    override val type = func.retType
    val name = func.name
}

// <pair-elem>
sealed class PairElem {
    abstract val expr: Expr
    abstract fun fetchType(scope: Scope): Option<Type>

    companion object {
        fun fst(expr: Expr) = Fst(expr)
        fun snd(expr: Expr) = Snd(expr)
    }
}

data class Fst internal constructor(override val expr: Expr) : PairElem() {
    override fun fetchType(scope: Scope): Option<Type> {
        val type = expr.type
        return if (type is PairT) {
            Some(type.fstT)
        } else {
            None
        }
    }
}

data class Snd internal constructor(override val expr: Expr) : PairElem() {
    override fun fetchType(scope: Scope): Option<Type> {
        val type = expr.type
        return if (type is PairT) {
            Some(type.sndT)
        } else {
            None
        }
    }
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class Ident(val name: String) {
    constructor(node: TerminalNode) : this(node.text)

    override fun toString() = name
}
