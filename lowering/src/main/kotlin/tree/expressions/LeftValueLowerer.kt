package me.eriknikli.rhenium.lowering.tree.expressions

import me.eriknikli.rhenium.ast.tree.expressions.Identifier
import me.eriknikli.rhenium.ast.tree.expressions.LeftValue
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.VarRefAction
import javax.inject.Inject
import javax.inject.Singleton

interface ILeftValueLowerer : INodeLowerer<LeftValue, VarRefAction>

@Singleton
class LeftValueLowerer
@Inject
constructor() : ILeftValueLowerer {
    override fun lower(node: LeftValue): VarRefAction {
        return when (node) {
            is Identifier -> VarRefAction(node.context.symbol.cName)
            else -> throw IllegalStateException("Illegal left value cannot be lowered: $node")
        }
    }
}
