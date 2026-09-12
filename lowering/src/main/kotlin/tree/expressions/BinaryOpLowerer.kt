package me.eriknikli.rhenium.lowering.tree.expressions

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.expressions.Expression
import me.eriknikli.rhenium.ast.tree.expressions.operators.BinaryOpExpression
import me.eriknikli.rhenium.ast.tree.expressions.operators.Operator
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.Action
import me.eriknikli.rhenium.lowering.actions.BinaryAction
import me.eriknikli.rhenium.lowering.actions.CastAction
import me.eriknikli.rhenium.semanticContext.scope.types.SignedIntType
import me.eriknikli.rhenium.semanticContext.scope.types.UnsignedIntType
import me.eriknikli.rhenium.semanticContext.scope.types.isNumeric
import javax.inject.Inject
import javax.inject.Singleton

interface IBinaryOpLowerer : INodeLowerer<BinaryOpExpression, Action>

@Singleton
class BinaryOpLowerer
@Inject
constructor() : IBinaryOpLowerer {
    @Inject
    lateinit var expressionLowererProvider: Lazy<IExpressionLowerer>

    private val expressionLowerer by lazy { expressionLowererProvider.get() }

    override fun lower(node: BinaryOpExpression): Action {
        val type = node.context.type
        val operator = node.operator.cString

        if (!type.isNumeric()) {
            return BinaryAction(
                operator,
                expressionLowerer.lower(node.left),
                expressionLowerer.lower(node.right)
            )
        }

        val detour = if (type is SignedIntType && node.operator in DETOURED_OPERATORS) type.detourType else null

        return CastAction(
            type,
            BinaryAction(operator, node.left.lowerOperand(detour), node.right.lowerOperand(detour))
        )
    }

    private fun Expression.lowerOperand(detour: UnsignedIntType?): Action {
        val operand = expressionLowerer.lower(this)

        return if (detour == null) operand else CastAction(detour, operand)
    }
}

private val DETOURED_OPERATORS = setOf(Operator.PLUS, Operator.MINUS, Operator.STAR)
