package me.eriknikli.rhenium.semanticAnalyzer.expressions

import arrow.core.leftNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.expressions.operators.BinaryOpExpression
import me.eriknikli.rhenium.ast.tree.expressions.operators.Operator
import me.eriknikli.rhenium.common.diagnostics.Diagnosed
import me.eriknikli.rhenium.semanticAnalyzer.diagnostics.BinaryOperatorTypeMismatch
import me.eriknikli.rhenium.semanticAnalyzer.diagnostics.IllegalBinaryOperation
import me.eriknikli.rhenium.semanticAnalyzer.diagnostics.MixedSignedness
import me.eriknikli.rhenium.semanticContext.scope.types.*
import javax.inject.Inject
import javax.inject.Singleton

interface IBinaryOpNodeDecorator {
    fun decorate(
        binaryOpExpression: BinaryOpExpression,
        expressionNodeDecoratorContext: ExpressionNodeDecoratorContext
    ): Diagnosed<ExpressionType>
}

@Singleton
class BinaryOpNodeDecorator
@Inject
constructor() : IBinaryOpNodeDecorator {
    @Inject
    lateinit var expressionNodeDecoratorProvider: Lazy<IExpressionNodeDecorator>

    private val expressionNodeDecorator by lazy { expressionNodeDecoratorProvider.get() }

    override fun decorate(
        binaryOpExpression: BinaryOpExpression,
        expressionNodeDecoratorContext: ExpressionNodeDecoratorContext
    ): Diagnosed<ExpressionType> = either {
        val scope = expressionNodeDecoratorContext.scope

        val (leftType, rightType) = zipOrAccumulate(
            {
                expressionNodeDecorator
                    .decorateExpression(binaryOpExpression.left, ExpressionNodeDecoratorContext(scope))
                    .bindNel()
            },
            {
                expressionNodeDecorator
                    .decorateExpression(binaryOpExpression.right, ExpressionNodeDecoratorContext(scope))
                    .bindNel()
            }
        ) { left, right -> left to right }

        val type = resolveType(leftType, rightType, binaryOpExpression.operator, binaryOpExpression).bind()

        binaryOpExpression.context.type = type

        type
    }

    private fun resolveType(
        left: ExpressionType,
        right: ExpressionType,
        operator: Operator,
        expression: BinaryOpExpression
    ): Diagnosed<ExpressionType> {
        if (left is InvalidType || right is InvalidType) {
            return InvalidType.right()
        }

        if (!left.isNumeric() || !right.isNumeric()) {
            return IllegalBinaryOperation(expression.parserContext, left, right, operator).leftNel()
        }

        return when (operator) {
            Operator.HAT -> {
                if (left !is FloatType && right !is FloatType) {
                    return IllegalBinaryOperation(expression.parserContext, left, right, operator).leftNel()
                }

                if (left == FloatType.F32 && right == FloatType.F32) {
                    FloatType.F32.right()
                } else {
                    FloatType.F64.right()
                }
            }

            Operator.STAR, Operator.SLASH, Operator.PERCENT, Operator.PLUS, Operator.MINUS -> {
                if (operator == Operator.PERCENT && (left is FloatType || right is FloatType)) {
                    return IllegalBinaryOperation(expression.parserContext, left, right, operator).leftNel()
                }

                if (isMixedSignedness(left, right)) {
                    return MixedSignedness(expression.parserContext, left, right, operator).leftNel()
                }

                arithmeticType(left, right)?.right()
                    ?: IllegalBinaryOperation(expression.parserContext, left, right, operator).leftNel()
            }

            Operator.EQUALS, Operator.NOT_EQUALS -> BooleanType.right()

            Operator.GREATER,
            Operator.GREATER_EQUALS,
            Operator.LESS,
            Operator.LESS_EQUALS -> BooleanType.right()

            else -> IllegalBinaryOperation(expression.parserContext, left, right, operator).leftNel()
        }
    }

    private fun isMixedSignedness(left: ExpressionType, right: ExpressionType): Boolean =
        (left is SignedIntType && right is UnsignedIntType) ||
                (left is UnsignedIntType && right is SignedIntType)

    private fun arithmeticType(left: ExpressionType, right: ExpressionType): ExpressionType? {
        if (left is SignedIntType && right is SignedIntType) {
            return if (left.index > right.index) right else left
        }
        if (left is UnsignedIntType && right is UnsignedIntType) {
            return if (left.index > right.index) right else left
        }
        if (left is FloatType && right is FloatType) {
            return if (left.index > right.index) right else left
        }
        if (left is FloatType) {
            return left
        }
        if (right is FloatType) {
            return right
        }

        return null
    }
}
