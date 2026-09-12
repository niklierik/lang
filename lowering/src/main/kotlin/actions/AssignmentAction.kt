package me.eriknikli.rhenium.lowering.actions

data class AssignmentAction(val target: Action, val value: Action) : Action
