package me.eriknikli.rhenium.lowering.actions

data class TernaryAction(
    val condition: Action,
    val ifTrue: Action,
    val ifFalse: Action
) : Action
