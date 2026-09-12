package me.eriknikli.rhenium.lowering.actions

data class PrintAction(val cFormat: String, val value: Action?) : Action
