package me.eriknikli.rhenium.lowering.actions

import me.eriknikli.rhenium.semanticContext.scope.types.ExpressionType

data class VarDeclarationAction(
    val type: ExpressionType,
    val cName: String,
    val value: Action
) : Action
