package me.eriknikli.rhenium.lowering

import me.eriknikli.rhenium.ast.tree.AstNode
import me.eriknikli.rhenium.lowering.actions.Action

interface INodeLowerer<T : AstNode, out A : Action> {
    fun lower(node: T): A
}
