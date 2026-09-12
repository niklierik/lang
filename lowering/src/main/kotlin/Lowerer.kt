package me.eriknikli.rhenium.lowering

import dagger.Lazy
import me.eriknikli.rhenium.ast.tree.RootNode
import me.eriknikli.rhenium.lowering.actions.Block
import me.eriknikli.rhenium.lowering.actions.ConstantAction
import me.eriknikli.rhenium.lowering.actions.FunctionAction
import me.eriknikli.rhenium.lowering.actions.ReturnAction
import me.eriknikli.rhenium.lowering.tree.statements.IStatementLowerer
import javax.inject.Inject
import javax.inject.Singleton

interface ILowerer : INodeLowerer<RootNode, FunctionAction>

@Singleton
class Lowerer
@Inject
constructor() : ILowerer {
    @Inject
    lateinit var statementLowererProvider: Lazy<IStatementLowerer>

    private val statementLowerer by lazy { statementLowererProvider.get() }

    override fun lower(node: RootNode): FunctionAction {
        val body = Block(mutableListOf())

        for (statement in node.statements) {
            body.actions.add(statementLowerer.lower(statement))
        }
        body.actions.add(ReturnAction(ConstantAction("0")))

        return FunctionAction("main", "int", body)
    }
}
