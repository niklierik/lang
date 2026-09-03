package me.eriknikli.rhenium.transpiler.tree.expressions

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.expressions.operators.Operator
import me.eriknikli.rhenium.ast.tree.expressions.operators.UnaryOpExpression
import me.eriknikli.rhenium.semanticContext.scope.types.SignedIntType
import me.eriknikli.rhenium.semanticContext.scope.types.isNumeric
import me.eriknikli.rhenium.transpiler.INodeTranspiler
import me.eriknikli.rhenium.transpiler.utils.writeText
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface IUnaryOpTranspiler : INodeTranspiler<UnaryOpExpression>

@Singleton
class CUnaryOpTranspiler
@Inject constructor() : IUnaryOpTranspiler {
    @Inject
    lateinit var expressionTranspilerProvider: Lazy<IExpressionTranspiler>

    private val expressionTranspiler by lazy { expressionTranspilerProvider.get() }

    override fun transpile(node: UnaryOpExpression, output: OutputStream) {
        val type = node.context.type

        if (!type.isNumeric()) {
            output.writeText("(")
            output.writeText(node.operator.cString)
            expressionTranspiler.transpile(node.expression, output)
            output.writeText(")")
            return
        }

        val detour = if (type is SignedIntType && node.operator == Operator.MINUS) type.unsigned else null

        output.writeText("(${type.cName})(")
        output.writeText(node.operator.cString)

        if (detour == null) {
            expressionTranspiler.transpile(node.expression, output)
        } else {
            output.writeText("(${detour.cName})(")
            expressionTranspiler.transpile(node.expression, output)
            output.writeText(")")
        }

        output.writeText(")")
    }
}
