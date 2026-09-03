package me.eriknikli.rhenium.transpiler.tree.expressions

import me.eriknikli.rhenium.ast.tree.expressions.literals.*
import me.eriknikli.rhenium.semanticContext.scope.types.UnsignedIntType
import me.eriknikli.rhenium.transpiler.INodeTranspiler
import me.eriknikli.rhenium.transpiler.utils.writeText
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface ILiteralExpressionTranspiler : INodeTranspiler<Literal<*>>

@Singleton
class CLiteralExpressionTranspiler
@Inject
constructor() : ILiteralExpressionTranspiler {
    override fun transpile(literal: Literal<*>, output: OutputStream) {
        when (literal) {
            is BooleanLiteral -> output.writeText(literal.value.toString())

            is U64Literal, is U32Literal, is U16Literal, is U8Literal,
            is I64Literal, is I32Literal, is I16Literal, is I8Literal,
            is F64Literal, is F32Literal -> output.writeText(literal.asNumericConstant())

            else -> throw IllegalStateException("Illegal literal $literal cannot be transpiled.")
        }
    }

    private fun Literal<*>.asNumericConstant(): String {
        val type = context.type
        val suffix = if (type is UnsignedIntType) "u" else ""

        return "(${type.cName})$value$suffix"
    }
}
