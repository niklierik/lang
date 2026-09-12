package me.eriknikli.rhenium.lowering.tree.statements

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.statements.PrintStatement
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.CastAction
import me.eriknikli.rhenium.lowering.actions.ConstantAction
import me.eriknikli.rhenium.lowering.actions.PrintAction
import me.eriknikli.rhenium.lowering.actions.TernaryAction
import me.eriknikli.rhenium.lowering.tree.expressions.IExpressionLowerer
import me.eriknikli.rhenium.semanticContext.scope.types.BooleanType
import javax.inject.Inject
import javax.inject.Singleton

interface IPrintStatementLowerer : INodeLowerer<PrintStatement, PrintAction>

@Singleton
class PrintStatementLowerer
@Inject
constructor() : IPrintStatementLowerer {
    @Inject
    lateinit var expressionLowerer: Lazy<IExpressionLowerer>

    override fun lower(node: PrintStatement): PrintAction {
        val expression = node.expression ?: return PrintAction(LINE_BREAK, null)

        val type = expression.context.type
        val format = type.cFormat
            ?: throw IllegalStateException("$type has no printf format and cannot be printed.")

        val value = expressionLowerer.get().lower(expression)

        return PrintAction(
            if (node.newLine) "$format $LINE_BREAK" else format,
            if (type is BooleanType) {
                TernaryAction(value, ConstantAction(TRUE), ConstantAction(FALSE))
            } else {
                CastAction(type, value)
            }
        )
    }
}

private const val LINE_BREAK = "\"\\n\""
private const val TRUE = "\"true\""
private const val FALSE = "\"false\""
