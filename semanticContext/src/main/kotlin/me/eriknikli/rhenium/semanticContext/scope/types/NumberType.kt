package me.eriknikli.rhenium.semanticContext.scope.types

import org.antlr.v4.runtime.ParserRuleContext

enum class SignedIntType(
    val index: Int,
    override val cName: String,
    override val cFormat: String
) : ExpressionType {
    I64(0, "int64_t", "\"%\" PRId64"),
    I32(1, "int32_t", "\"%\" PRId32"),
    I16(2, "int16_t", "\"%d\""),
    I8(3, "int8_t", "\"%d\"");

    val detourType: UnsignedIntType
        get() = when (this) {
            I64 -> UnsignedIntType.U64
            I32, I16, I8 -> UnsignedIntType.U32
        }

    override val declarationParserContext: ParserRuleContext? = null

    override fun canAssignTo(target: ExpressionType): Boolean {
        if (target is InvalidType) {
            return true
        }
        if (target !is SignedIntType) {
            return false
        }
        return index >= target.index
    }

    override fun canAssignToExplicit(target: ExpressionType): Boolean {
        return target is InvalidType || target is FloatType || target is SignedIntType || target is UnsignedIntType
    }
}

enum class UnsignedIntType(
    val index: Int,
    override val cName: String,
    override val cFormat: String
) : ExpressionType {
    U64(0, "uint64_t", "\"%\" PRIu64"),
    U32(1, "uint32_t", "\"%\" PRIu32"),
    U16(2, "uint16_t", "\"%u\""),
    U8(3, "uint8_t", "\"%u\"");

    override val declarationParserContext: ParserRuleContext? = null

    override fun canAssignTo(target: ExpressionType): Boolean {
        if (target is InvalidType) {
            return true
        }
        if (target !is UnsignedIntType) {
            return false
        }
        return index >= target.index
    }

    override fun canAssignToExplicit(target: ExpressionType): Boolean {
        return target is InvalidType || target is FloatType || target is SignedIntType || target is UnsignedIntType
    }
}

enum class FloatType(
    val index: Int,
    override val cName: String,
    override val cFormat: String
) : ExpressionType {
    F64(0, "float64_t", "\"%f\""),
    F32(1, "float32_t", "\"%f\"");

    override val declarationParserContext: ParserRuleContext? = null

    override fun canAssignTo(target: ExpressionType): Boolean {
        if (target is InvalidType) {
            return true
        }
        if (target !is FloatType) {
            return false
        }
        return index >= target.index
    }

    override fun canAssignToExplicit(target: ExpressionType): Boolean {
        return target is InvalidType || target is FloatType || target is SignedIntType || target is UnsignedIntType
    }
}

fun numericTypes(): Set<ExpressionType> {
    val signed = SignedIntType.entries.toSet()
    val unsigned = UnsignedIntType.entries.toSet()
    val float = FloatType.entries.toSet()

    return signed.union(unsigned).union(float)
}