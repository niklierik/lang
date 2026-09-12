package me.eriknikli.rhenium.lowering.actions

data class UnaryAction(val cOperator: String, val operand: Action) : Action
