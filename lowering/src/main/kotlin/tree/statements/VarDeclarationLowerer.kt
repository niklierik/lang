package me.eriknikli.rhenium.lowering.tree.statements

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.statements.vars.VarDeclarationStatement
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.VarDeclarationAction
import me.eriknikli.rhenium.lowering.tree.expressions.IExpressionLowerer
import javax.inject.Inject
import javax.inject.Singleton

interface IVarDeclarationLowerer : INodeLowerer<VarDeclarationStatement, VarDeclarationAction>

@Singleton
class VarDeclarationLowerer
@Inject
constructor() : IVarDeclarationLowerer {
    @Inject
    lateinit var expressionLowerer: Lazy<IExpressionLowerer>

    override fun lower(node: VarDeclarationStatement): VarDeclarationAction {
        val context = node.context

        return VarDeclarationAction(
            context.typeToDeclare,
            context.symbolInfo.cName,
            expressionLowerer.get().lower(node.rightSide)
        )
    }
}
