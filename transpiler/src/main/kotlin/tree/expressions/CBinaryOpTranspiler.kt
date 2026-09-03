package me.eriknikli.rhenium.transpiler.tree.expressions

import me.eriknikli.rhenium.ast.tree.expressions.Expression
import me.eriknikli.rhenium.ast.tree.expressions.operators.BinaryOpExpression
import me.eriknikli.rhenium.ast.tree.expressions.operators.Operator
import me.eriknikli.rhenium.semanticContext.scope.types.SignedIntType
import me.eriknikli.rhenium.semanticContext.scope.types.UnsignedIntType
import me.eriknikli.rhenium.semanticContext.scope.types.isNumeric
import me.eriknikli.rhenium.transpiler.INodeTranspiler
import me.eriknikli.rhenium.transpiler.utils.writeText
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface IBinaryOpTranspiler : INodeTranspiler<BinaryOpExpression>

@Singleton
class CBinaryOpTranspiler
@Inject
constructor() : IBinaryOpTranspiler {
    @Inject
    lateinit var expressionTranspilerProvider: dagger.Lazy<IExpressionTranspiler>

    private val expressionTranspiler by lazy { expressionTranspilerProvider.get() }

    override fun transpile(node: BinaryOpExpression, output: OutputStream) {
        val type = node.context.type

        if (!type.isNumeric()) {
            output.writeText("(")
            expressionTranspiler.transpile(node.left, output)
            output.writeText(node.operator.cString)
            expressionTranspiler.transpile(node.right, output)
            output.writeText(")")
            return
        }

        val detour = if (type is SignedIntType && node.operator in DETOURED_OPERATORS) type.unsigned else null

        output.writeText("(${type.cName})(")
        transpileOperand(node.left, detour, output)
        output.writeText(node.operator.cString)
        transpileOperand(node.right, detour, output)
        output.writeText(")")
    }

    private fun transpileOperand(operand: Expression, detour: UnsignedIntType?, output: OutputStream) {
        if (detour == null) {
            expressionTranspiler.transpile(operand, output)
            return
        }

        output.writeText("(${detour.cName})(")
        expressionTranspiler.transpile(operand, output)
        output.writeText(")")
    }
}

private val DETOURED_OPERATORS = setOf(Operator.PLUS, Operator.MINUS, Operator.STAR)
