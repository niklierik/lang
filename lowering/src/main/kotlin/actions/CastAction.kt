package me.eriknikli.rhenium.lowering.actions

import me.eriknikli.rhenium.semanticContext.scope.types.ExpressionType

data class CastAction(val type: ExpressionType, val operand: Action) : Action
