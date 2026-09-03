package me.eriknikli.rhenium.semanticAnalyzer.diagnostics

import me.eriknikli.rhenium.ast.tree.expressions.operators.Operator
import me.eriknikli.rhenium.common.diagnostics.ContextDiagnostic
import me.eriknikli.rhenium.semanticContext.scope.types.ExpressionType
import org.antlr.v4.runtime.ParserRuleContext

data class MixedSignedness(
    override val parserContext: ParserRuleContext,
    val left: ExpressionType,
    val right: ExpressionType,
    val operator: Operator
) : ContextDiagnostic {
    override val message: String =
        "cannot mix signed and unsigned operands in '$left ${operator.cString} $right'."
}
