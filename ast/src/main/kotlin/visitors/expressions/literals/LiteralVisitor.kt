package me.eriknikli.rhenium.ast.visitors.expressions.literals

import arrow.core.leftNel
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import me.eriknikli.rhenium.ast.diagnostics.InvalidValueOfLiteral
import me.eriknikli.rhenium.ast.diagnostics.UnexpectedPrimitiveType
import me.eriknikli.rhenium.ast.diagnostics.UnhandledParseRule
import me.eriknikli.rhenium.ast.tree.expressions.literals.*
import me.eriknikli.rhenium.common.diagnostics.Diagnosed
import me.eriknikli.rhenium.parser.RheniumParser
import me.eriknikli.rhenium.parser.RheniumParserBaseVisitor
import org.antlr.v4.runtime.ParserRuleContext
import javax.inject.Inject
import javax.inject.Singleton

interface ILiteralVisitor {
    fun visitLiteral(ctx: RheniumParser.LiteralContext): Diagnosed<Literal<*>>
}

@Singleton
class LiteralVisitor
@Inject
constructor() : RheniumParserBaseVisitor<Diagnosed<Literal<*>>>(), ILiteralVisitor {

    @Inject
    lateinit var literalTypeVisitor: ILiteralTypeVisitor

    override fun defaultResult(): Diagnosed<Literal<*>> = UnhandledParseRule.leftNel()

    override fun visitUnsigned(ctx: RheniumParser.UnsignedContext): Diagnosed<Literal<*>> = either {
        val type = literalTypeVisitor.visitUnsignedTypes(ctx.unsignedTypes()).bind()
        val text = ctx.UNSIGNED_INT().text

        when (type) {
            LiteralType.U8 -> U8Literal(ctx, text.parsedAs(ctx, type) { it.toUByte() }.bind(), text)
            LiteralType.U16 -> U16Literal(ctx, text.parsedAs(ctx, type) { it.toUShort() }.bind(), text)
            LiteralType.U32 -> U32Literal(ctx, text.parsedAs(ctx, type) { it.toUInt() }.bind(), text)
            LiteralType.U64 -> U64Literal(ctx, text.parsedAs(ctx, type) { it.toULong() }.bind(), text)
            else -> raise(
                UnexpectedPrimitiveType(
                    ctx,
                    type,
                    listOf(LiteralType.U8, LiteralType.U16, LiteralType.U32, LiteralType.U64)
                ).nel()
            )
        }
    }

    override fun visitSigned(ctx: RheniumParser.SignedContext): Diagnosed<Literal<*>> = either {
        val type = literalTypeVisitor.visitSignedTypes(ctx.signedTypes()).bind()
        val text = (ctx.SIGNED_INT() ?: ctx.UNSIGNED_INT()).text

        when (type) {
            LiteralType.I8 -> I8Literal(ctx, text.parsedAs(ctx, type) { it.toByte() }.bind(), text)
            LiteralType.I16 -> I16Literal(ctx, text.parsedAs(ctx, type) { it.toShort() }.bind(), text)
            LiteralType.I32 -> I32Literal(ctx, text.parsedAs(ctx, type) { it.toInt() }.bind(), text)
            LiteralType.I64 -> I64Literal(ctx, text.parsedAs(ctx, type) { it.toLong() }.bind(), text)
            else -> raise(
                UnexpectedPrimitiveType(
                    ctx,
                    type,
                    listOf(LiteralType.I8, LiteralType.I16, LiteralType.I32, LiteralType.I64)
                ).nel()
            )
        }
    }

    override fun visitFloat(ctx: RheniumParser.FloatContext): Diagnosed<Literal<*>> = either {
        val type = literalTypeVisitor.visitFloatTypes(ctx.floatTypes()).bind()
        val text = (ctx.FLOAT() ?: ctx.SIGNED_INT() ?: ctx.UNSIGNED_INT()).text

        when (type) {
            LiteralType.F32 -> F32Literal(ctx, text.parsedAs(ctx, type) { it.toFloat() }.bind(), text)
            LiteralType.F64 -> F64Literal(ctx, text.parsedAs(ctx, type) { it.toDouble() }.bind(), text)
            else -> raise(
                UnexpectedPrimitiveType(ctx, type, listOf(LiteralType.F32, LiteralType.F64)).nel()
            )
        }
    }

    override fun visitUnsignedBasic(ctx: RheniumParser.UnsignedBasicContext): Diagnosed<Literal<*>> {
        val text = ctx.UNSIGNED_INT().text
        return text.parsedAs(ctx, LiteralType.I32) { it.toInt() }.map { I32Literal(ctx, it, text) }
    }

    override fun visitSignedBasic(ctx: RheniumParser.SignedBasicContext): Diagnosed<Literal<*>> {
        val text = ctx.SIGNED_INT().text
        return text.parsedAs(ctx, LiteralType.I32) { it.toInt() }.map { I32Literal(ctx, it, text) }
    }

    override fun visitFloatBasic(ctx: RheniumParser.FloatBasicContext): Diagnosed<Literal<*>> {
        val text = ctx.FLOAT().text
        return text.parsedAs(ctx, LiteralType.F64) { it.toDouble() }.map { F64Literal(ctx, it, text) }
    }

    override fun visitBooleanLiteral(ctx: RheniumParser.BooleanLiteralContext): Diagnosed<Literal<*>> {
        val text = ctx.text

        return when (text) {
            "true" -> BooleanLiteral(ctx, true, "true").right()
            "false" -> BooleanLiteral(ctx, false, "false").right()
            else -> InvalidValueOfLiteral(ctx, text, LiteralType.BOOL).leftNel()
        }
    }

    private inline fun <T> String.parsedAs(
        ctx: ParserRuleContext,
        type: LiteralType,
        parse: (String) -> T
    ): Diagnosed<T> =
        try {
            val value = parse(this)

            if (value.isNonFiniteFloat()) {
                InvalidValueOfLiteral(ctx, this, type).leftNel()
            } else {
                value.right()
            }
        } catch (_: NumberFormatException) {
            InvalidValueOfLiteral(ctx, this, type).leftNel()
        }

    private fun Any?.isNonFiniteFloat(): Boolean =
        (this is Float && !isFinite()) || (this is Double && !isFinite())
}
