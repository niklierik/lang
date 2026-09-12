package me.eriknikli.rhenium.lowering.tree.expressions

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.expressions.Expression
import me.eriknikli.rhenium.ast.tree.expressions.LeftValue
import me.eriknikli.rhenium.ast.tree.expressions.literals.Literal
import me.eriknikli.rhenium.ast.tree.expressions.operators.BinaryOpExpression
import me.eriknikli.rhenium.ast.tree.expressions.operators.UnaryOpExpression
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.Action
import javax.inject.Inject
import javax.inject.Singleton

interface IExpressionLowerer : INodeLowerer<Expression, Action>

@Singleton
class ExpressionLowerer
@Inject
constructor() : IExpressionLowerer {
    @Inject
    lateinit var literalLowerer: Lazy<ILiteralLowerer>

    @Inject
    lateinit var leftValueLowerer: Lazy<ILeftValueLowerer>

    @Inject
    lateinit var binaryOpLowerer: Lazy<IBinaryOpLowerer>

    @Inject
    lateinit var unaryOpLowerer: Lazy<IUnaryOpLowerer>

    override fun lower(node: Expression): Action {
        return when (node) {
            is Literal<*> -> literalLowerer.get().lower(node)
            is UnaryOpExpression -> unaryOpLowerer.get().lower(node)
            is BinaryOpExpression -> binaryOpLowerer.get().lower(node)
            is LeftValue -> leftValueLowerer.get().lower(node)
            else -> throw IllegalStateException("Unhandled node ${node.javaClass} and cannot lower it as expression.")
        }
    }
}
