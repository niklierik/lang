package me.eriknikli.rhenium.lowering.tree.statements

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.statements.ExpressionStatement
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.ExpressionStatementAction
import me.eriknikli.rhenium.lowering.tree.expressions.IExpressionLowerer
import javax.inject.Inject
import javax.inject.Singleton

interface IExpressionStatementLowerer : INodeLowerer<ExpressionStatement, ExpressionStatementAction>

@Singleton
class ExpressionStatementLowerer
@Inject
constructor() : IExpressionStatementLowerer {
    @Inject
    lateinit var expressionLowerer: Lazy<IExpressionLowerer>

    override fun lower(node: ExpressionStatement): ExpressionStatementAction {
        return ExpressionStatementAction(expressionLowerer.get().lower(node.expression))
    }
}
