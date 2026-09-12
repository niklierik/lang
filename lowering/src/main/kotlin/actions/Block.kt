package me.eriknikli.rhenium.lowering.actions

data class Block(val actions: MutableList<Action>) : Action
