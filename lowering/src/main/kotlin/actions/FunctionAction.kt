package me.eriknikli.rhenium.lowering.actions

data class FunctionAction(
    val cName: String,
    val cReturnType: String,
    val body: Block
) : Action
