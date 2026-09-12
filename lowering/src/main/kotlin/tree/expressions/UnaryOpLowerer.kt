package me.eriknikli.rhenium.lowering.tree.expressions

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.expressions.operators.Operator
import me.eriknikli.rhenium.ast.tree.expressions.operators.UnaryOpExpression
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.Action
import me.eriknikli.rhenium.lowering.actions.CastAction
import me.eriknikli.rhenium.lowering.actions.UnaryAction
import me.eriknikli.rhenium.semanticContext.scope.types.SignedIntType
import me.eriknikli.rhenium.semanticContext.scope.types.isNumeric
import javax.inject.Inject
import javax.inject.Singleton

interface IUnaryOpLowerer : INodeLowerer<UnaryOpExpression, Action>

@Singleton
class UnaryOpLowerer
@Inject
constructor() : IUnaryOpLowerer {
    @Inject
    lateinit var expressionLowererProvider: Lazy<IExpressionLowerer>

    private val expressionLowerer by lazy { expressionLowererProvider.get() }

    override fun lower(node: UnaryOpExpression): Action {
        val type = node.context.type
        val operator = node.operator.cString
        val operand = expressionLowerer.lower(node.expression)

        if (!type.isNumeric()) {
            return UnaryAction(operator, operand)
        }

        val detour = if (type is SignedIntType && node.operator == Operator.MINUS) type.detourType else null

        return CastAction(
            type,
            UnaryAction(operator, if (detour == null) operand else CastAction(detour, operand))
        )
    }
}
