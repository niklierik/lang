package me.eriknikli.rhenium.transpiler

import me.eriknikli.rhenium.lowering.actions.*
import me.eriknikli.rhenium.transpiler.utils.writeLineBreak
import me.eriknikli.rhenium.transpiler.utils.writeText
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface ITranspiler {
    fun transpile(action: Action, outputStream: OutputStream)
}

@Singleton
class CTranspiler
@Inject
constructor() : ITranspiler {
    override fun transpile(action: Action, outputStream: OutputStream) {
        outputStream.writeText(PROLOGUE)
        outputStream.writeLineBreak()
        outputStream.writeLineBreak()

        action.write(outputStream)
    }

    private fun Action.write(output: OutputStream) {
        when (this) {
            is Block -> actions.forEach { it.write(output) }

            is FunctionAction -> {
                output.writeText("$cReturnType $cName(){")
                body.write(output)
                output.writeText("}")
            }

            is ReturnAction -> {
                output.writeText("return")
                value?.let {
                    output.writeText(" ")
                    it.write(output)
                }
                output.writeText(";")
            }

            is PrintAction -> {
                output.writeText("printf($cFormat")
                value?.let {
                    output.writeText(",")
                    it.write(output)
                }
                output.writeText(");")
            }

            is CastAction -> {
                output.writeText("((${type.cName})")
                operand.write(output)
                output.writeText(")")
            }

            is TernaryAction -> {
                output.writeText("(")
                condition.write(output)
                output.writeText("?")
                ifTrue.write(output)
                output.writeText(":")
                ifFalse.write(output)
                output.writeText(")")
            }

            is BinaryAction -> {
                output.writeText("(")
                left.write(output)
                output.writeText(cOperator)
                right.write(output)
                output.writeText(")")
            }

            is UnaryAction -> {
                output.writeText("($cOperator")
                operand.write(output)
                output.writeText(")")
            }

            is VarDeclarationAction -> {
                output.writeText("${type.cName} $cName=")
                value.write(output)
                output.writeText(";")
            }

            is AssignmentAction -> {
                target.write(output)
                output.writeText("=")
                value.write(output)
                output.writeText(";")
            }

            is ExpressionStatementAction -> {
                value.write(output)
                output.writeText(";")
            }

            is VarRefAction -> output.writeText(cName)

            is ConstantAction -> output.writeText(cLiteral)
        }
    }
}

private val PROLOGUE = """
    #include <math.h>
    #include <stdio.h>
    #include <stdlib.h>
    #include <stdint.h>
    #include <stdbool.h>
    #include <inttypes.h>

    typedef _Float32 float32_t;
    typedef _Float64 float64_t;
    typedef _Bool boolean_t;
""".trimIndent()
