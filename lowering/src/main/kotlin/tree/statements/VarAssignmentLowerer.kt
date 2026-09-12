package me.eriknikli.rhenium.lowering.tree.statements

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.statements.vars.VarAssignmentStatement
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.AssignmentAction
import me.eriknikli.rhenium.lowering.tree.expressions.IExpressionLowerer
import me.eriknikli.rhenium.lowering.tree.expressions.ILeftValueLowerer
import javax.inject.Inject
import javax.inject.Singleton

interface IVarAssignmentLowerer : INodeLowerer<VarAssignmentStatement, AssignmentAction>

@Singleton
class VarAssignmentLowerer
@Inject
constructor() : IVarAssignmentLowerer {
    @Inject
    lateinit var expressionLowerer: Lazy<IExpressionLowerer>

    @Inject
    lateinit var leftValueLowerer: Lazy<ILeftValueLowerer>

    override fun lower(node: VarAssignmentStatement): AssignmentAction {
        return AssignmentAction(
            leftValueLowerer.get().lower(node.leftValue),
            expressionLowerer.get().lower(node.rightValue)
        )
    }
}
