package me.eriknikli.rhenium.lowering.actions

data class BinaryAction(
    val cOperator: String,
    val left: Action,
    val right: Action
) : Action
