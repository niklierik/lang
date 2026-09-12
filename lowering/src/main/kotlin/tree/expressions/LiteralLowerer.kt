package me.eriknikli.rhenium.lowering.tree.expressions

import me.eriknikli.rhenium.ast.tree.expressions.literals.*
import me.eriknikli.rhenium.lowering.INodeLowerer
import me.eriknikli.rhenium.lowering.actions.Action
import me.eriknikli.rhenium.lowering.actions.CastAction
import me.eriknikli.rhenium.lowering.actions.ConstantAction
import me.eriknikli.rhenium.semanticContext.scope.types.UnsignedIntType
import javax.inject.Inject
import javax.inject.Singleton

interface ILiteralLowerer : INodeLowerer<Literal<*>, Action>

@Singleton
class LiteralLowerer
@Inject
constructor() : ILiteralLowerer {
    override fun lower(node: Literal<*>): Action {
        return when (node) {
            is BooleanLiteral -> ConstantAction(node.value.toString())

            is U64Literal, is U32Literal, is U16Literal, is U8Literal,
            is I64Literal, is I32Literal, is I16Literal, is I8Literal,
            is F64Literal, is F32Literal -> CastAction(node.context.type, ConstantAction(node.cLiteral()))

            else -> throw IllegalStateException("Illegal literal $node cannot be lowered.")
        }
    }

    private fun Literal<*>.cLiteral(): String {
        if (context.type is UnsignedIntType) {
            return "${value}u"
        }

        if (value == Long.MIN_VALUE) {
            return "(${Long.MIN_VALUE + 1}-1)"
        }

        return value.toString()
    }
}
