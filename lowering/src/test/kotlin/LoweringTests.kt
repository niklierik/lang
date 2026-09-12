import arrow.core.getOrElse
import me.eriknikli.rhenium.common.diagnostics.render
import me.eriknikli.rhenium.lowering.actions.*
import org.antlr.v4.runtime.CharStreams
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.fail

class LoweringTests {
    private val component = DaggerLoweringTestComponent.create()
    private val astBuilder = component.makeAstBuilder()
    private val semanticAnalyzer = component.makeSemanticAnalyzer()
    private val lowerer = component.makeLowerer()

    @ParameterizedTest(name = "Run {index}, name {0}")
    @MethodSource("provideData")
    fun `test lowered actions`(name: String, sourceCode: String, expectedActions: String) {
        val ast = astBuilder.parse(CharStreams.fromString(sourceCode))
            .getOrElse { fail("expected the source to parse, got:\n${it.render()}") }

        semanticAnalyzer.decorateSemanticContext(ast)
            .getOrElse { fail("expected the source to analyze, got:\n${it.render()}") }

        assertEquals(expectedActions, lowerer.lower(ast).body())
    }

    companion object {
        @JvmStatic
        fun provideData(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "print omits the line break",
                    "print 1;",
                    """(print "%" PRId32 (cast int32_t (cast int32_t 1)))"""
                ),
                Arguments.of(
                    "println appends a line break",
                    "println 1;",
                    """(print "%" PRId32 "\n" (cast int32_t (cast int32_t 1)))"""
                ),
                Arguments.of(
                    "bare println carries a format and no value",
                    "println;",
                    """(print "\n")"""
                ),
                Arguments.of(
                    "i8",
                    "println I8(-8);",
                    """(print "%d" "\n" (cast int8_t (cast int8_t -8)))"""
                ),
                Arguments.of(
                    "i16",
                    "println I16(-16);",
                    """(print "%d" "\n" (cast int16_t (cast int16_t -16)))"""
                ),
                Arguments.of(
                    "i32",
                    "println I32(-32);",
                    """(print "%" PRId32 "\n" (cast int32_t (cast int32_t -32)))"""
                ),
                Arguments.of(
                    "i64",
                    "println I64(-64);",
                    """(print "%" PRId64 "\n" (cast int64_t (cast int64_t -64)))"""
                ),
                Arguments.of(
                    "u8",
                    "println U8(8);",
                    """(print "%u" "\n" (cast uint8_t (cast uint8_t 8u)))"""
                ),
                Arguments.of(
                    "u16",
                    "println U16(16);",
                    """(print "%u" "\n" (cast uint16_t (cast uint16_t 16u)))"""
                ),
                Arguments.of(
                    "u32",
                    "println U32(32);",
                    """(print "%" PRIu32 "\n" (cast uint32_t (cast uint32_t 32u)))"""
                ),
                Arguments.of(
                    "u64",
                    "println U64(64);",
                    """(print "%" PRIu64 "\n" (cast uint64_t (cast uint64_t 64u)))"""
                ),
                Arguments.of(
                    "a u64 at its maximum keeps the u suffix that stops c reading it as signed",
                    "println U64(18446744073709551615);",
                    """(print "%" PRIu64 "\n" (cast uint64_t (cast uint64_t 18446744073709551615u)))"""
                ),
                Arguments.of(
                    "f32",
                    "println F32(1.5);",
                    """(print "%f" "\n" (cast float32_t (cast float32_t 1.5)))"""
                ),
                Arguments.of(
                    "f64",
                    "println F64(2.5);",
                    """(print "%f" "\n" (cast float64_t (cast float64_t 2.5)))"""
                ),
                Arguments.of(
                    "the most negative i64 never reaches c as an out-of-range constant",
                    "println I64(-9223372036854775808);",
                    """(print "%" PRId64 "\n" (cast int64_t (cast int64_t (-9223372036854775807-1))))"""
                ),
                Arguments.of(
                    "a boolean lowers to a ternary, so the printer never learns booleans are special",
                    "println true;",
                    """(print "%s" "\n" (?: true "true" "false"))"""
                ),
                Arguments.of(
                    "a declared boolean carries the boolean typedef",
                    "let a: Boolean = true;",
                    "(decl boolean_t re_a true)"
                ),
                Arguments.of(
                    "a declaration carries the declared type and the mangled name",
                    "let a = I64(42);",
                    "(decl int64_t re_a (cast int64_t 42))"
                ),
                Arguments.of(
                    "reading a variable lowers to a reference carrying only its mangled name",
                    "let a = I64(42);\nprintln a;",
                    """(decl int64_t re_a (cast int64_t 42)) (print "%" PRId64 "\n" (cast int64_t re_a))"""
                ),
                Arguments.of(
                    "assignment lowers to an assignment whose target is a reference",
                    "let a = I32(1);\na = I32(2);",
                    "(decl int32_t re_a (cast int32_t 1)) (= re_a (cast int32_t 2))"
                ),
                Arguments.of(
                    "an expression statement discards its value",
                    "I32(1);",
                    "(expr (cast int32_t 1))"
                ),
                Arguments.of(
                    "default integer literals are i32 and detour like any other signed addition",
                    "1 + 2;",
                    "(expr (cast int32_t (+ (cast uint32_t (cast int32_t 1)) (cast uint32_t (cast int32_t 2)))))"
                ),
                Arguments.of(
                    "signed addition wraps by detouring through the unsigned counterpart",
                    "I32(1) + I32(2);",
                    "(expr (cast int32_t (+ (cast uint32_t (cast int32_t 1)) (cast uint32_t (cast int32_t 2)))))"
                ),
                Arguments.of(
                    "signed subtraction takes the same detour",
                    "I32(1) - I32(2);",
                    "(expr (cast int32_t (- (cast uint32_t (cast int32_t 1)) (cast uint32_t (cast int32_t 2)))))"
                ),
                Arguments.of(
                    "signed multiplication takes the same detour",
                    "I8(3) * I8(4);",
                    "(expr (cast int8_t (* (cast uint32_t (cast int8_t 3)) (cast uint32_t (cast int8_t 4)))))"
                ),
                Arguments.of(
                    "a narrow detour widens to uint32_t, which c will not promote back to signed",
                    "I16(-1) * I16(-1);",
                    "(expr (cast int16_t (* (cast uint32_t (cast int16_t -1)) (cast uint32_t (cast int16_t -1)))))"
                ),
                Arguments.of(
                    "the widest signed type detours through the widest unsigned one",
                    "I64(3) * I64(4);",
                    "(expr (cast int64_t (* (cast uint64_t (cast int64_t 3)) (cast uint64_t (cast int64_t 4)))))"
                ),
                Arguments.of(
                    "signed division does not detour, because MIN / -1 would change answer not width",
                    "I32(6) / I32(2);",
                    "(expr (cast int32_t (/ (cast int32_t 6) (cast int32_t 2))))"
                ),
                Arguments.of(
                    "signed remainder does not detour either",
                    "I32(7) % I32(2);",
                    "(expr (cast int32_t (% (cast int32_t 7) (cast int32_t 2))))"
                ),
                Arguments.of(
                    "unsigned arithmetic needs no detour, only the result cast",
                    "U32(1) + U32(2);",
                    "(expr (cast uint32_t (+ (cast uint32_t 1u) (cast uint32_t 2u))))"
                ),
                Arguments.of(
                    "float arithmetic takes the result cast and no detour",
                    "F64(1.5) + F64(2.5);",
                    "(expr (cast float64_t (+ (cast float64_t 1.5) (cast float64_t 2.5))))"
                ),
                Arguments.of(
                    "unary minus on a signed type detours",
                    "-I32(5);",
                    "(expr (cast int32_t (- (cast uint32_t (cast int32_t 5)))))"
                ),
                Arguments.of(
                    "unary plus takes the result cast without a detour",
                    "+I32(5);",
                    "(expr (cast int32_t (+ (cast int32_t 5))))"
                ),
                Arguments.of(
                    "a comparison yields a boolean and takes no result cast",
                    "I32(1) < I32(2);",
                    "(expr (< (cast int32_t 1) (cast int32_t 2)))"
                ),
                Arguments.of(
                    "negating a boolean takes no result cast",
                    "!true;",
                    "(expr (! true))"
                ),
                Arguments.of(
                    "a boolean-valued operator stays nested, as the logical operators will (ADR 0002)",
                    "I32(1) == I32(2);",
                    "(expr (== (cast int32_t 1) (cast int32_t 2)))"
                ),
                Arguments.of(
                    "nested arithmetic composes the casts",
                    "(I32(1) + I32(2)) * I32(3);",
                    "(expr (cast int32_t (* (cast uint32_t (cast int32_t (+ (cast uint32_t (cast int32_t 1)) (cast uint32_t (cast int32_t 2))))) (cast uint32_t (cast int32_t 3)))))"
                ),
                Arguments.of(
                    "the most negative i64 is lowered so that c never sees an out-of-range constant",
                    "I64(-9223372036854775808);",
                    "(expr (cast int64_t (-9223372036854775807-1)))"
                )
            )
        }

        private fun FunctionAction.body(): String = body.actions
            .dropLast(1)
            .joinToString(" ") { it.sexpr() }
            .replace(Regex("re_a_[0-9a-f_]+"), "re_a")

        private fun Action.sexpr(): String = when (this) {
            is Block -> (listOf("block") + actions.map { it.sexpr() }).joinToString(" ", "(", ")")
            is FunctionAction -> "(fn $cReturnType $cName ${body.sexpr()})"
            is ReturnAction -> value?.let { "(return ${it.sexpr()})" } ?: "(return)"
            is PrintAction -> value?.let { "(print $cFormat ${it.sexpr()})" } ?: "(print $cFormat)"
            is CastAction -> "(cast ${type.cName} ${operand.sexpr()})"
            is TernaryAction -> "(?: ${condition.sexpr()} ${ifTrue.sexpr()} ${ifFalse.sexpr()})"
            is BinaryAction -> "($cOperator ${left.sexpr()} ${right.sexpr()})"
            is UnaryAction -> "($cOperator ${operand.sexpr()})"
            is VarDeclarationAction -> "(decl ${type.cName} $cName ${value.sexpr()})"
            is AssignmentAction -> "(= ${target.sexpr()} ${value.sexpr()})"
            is ExpressionStatementAction -> "(expr ${value.sexpr()})"
            is VarRefAction -> cName
            is ConstantAction -> cLiteral
        }
    }
}
