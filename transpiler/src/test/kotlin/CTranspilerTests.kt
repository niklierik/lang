import arrow.core.getOrElse
import me.eriknikli.rhenium.common.diagnostics.render
import org.antlr.v4.runtime.CharStreams
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.fail

class CTranspilerTests {
    private val component = DaggerTranspilerTestComponent.create()
    private val astBuilder = component.makeAstBuilder()
    private val semanticAnalyzer = component.makeSemanticAnalyzer()
    private val transpiler = component.makeTranspiler()

    @ParameterizedTest(name = "Run {index}, name {0}")
    @MethodSource("provideData")
    fun `test emitted c`(name: String, sourceCode: String, expectedBody: String) {
        val ast = astBuilder.parse(CharStreams.fromString(sourceCode))
            .getOrElse { fail("expected the source to parse, got:\n${it.render()}") }

        semanticAnalyzer.decorateSemanticContext(ast)
            .getOrElse { fail("expected the source to analyze, got:\n${it.render()}") }

        val output = ByteArrayOutputStream()
        transpiler.transpile(ast, output)

        assertEquals(expectedBody, output.toString().mainBody())
    }

    companion object {
        @JvmStatic
        fun provideData(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "print omits the line break",
                    "print 1;",
                    """printf("%" PRId32,(int32_t)((int32_t)1));"""
                ),
                Arguments.of(
                    "println appends a line break",
                    "println 1;",
                    """printf("%" PRId32 "\n",(int32_t)((int32_t)1));"""
                ),
                Arguments.of("bare println writes only a line break", "println;", """printf("\n");"""),
                Arguments.of("i8", "println I8(-8);", """printf("%d" "\n",(int8_t)((int8_t)-8));"""),
                Arguments.of("i16", "println I16(-16);", """printf("%d" "\n",(int16_t)((int16_t)-16));"""),
                Arguments.of(
                    "i32",
                    "println I32(-32);",
                    """printf("%" PRId32 "\n",(int32_t)((int32_t)-32));"""
                ),
                Arguments.of(
                    "i64",
                    "println I64(-64);",
                    """printf("%" PRId64 "\n",(int64_t)((int64_t)-64));"""
                ),
                Arguments.of("u8", "println U8(8);", """printf("%u" "\n",(uint8_t)((uint8_t)8u));"""),
                Arguments.of("u16", "println U16(16);", """printf("%u" "\n",(uint16_t)((uint16_t)16u));"""),
                Arguments.of(
                    "u32",
                    "println U32(32);",
                    """printf("%" PRIu32 "\n",(uint32_t)((uint32_t)32u));"""
                ),
                Arguments.of(
                    "u64",
                    "println U64(64);",
                    """printf("%" PRIu64 "\n",(uint64_t)((uint64_t)64u));"""
                ),
                Arguments.of(
                    "a u64 at its maximum keeps the u suffix that stops c reading it as signed",
                    "println U64(18446744073709551615);",
                    """printf("%" PRIu64 "\n",(uint64_t)((uint64_t)18446744073709551615u));"""
                ),
                Arguments.of("f32", "println F32(1.5);", """printf("%f" "\n",(float32_t)((float32_t)1.5));"""),
                Arguments.of("f64", "println F64(2.5);", """printf("%f" "\n",(float64_t)((float64_t)2.5));"""),
                Arguments.of(
                    "a boolean renders as its source spelling, not as an int",
                    "println true;",
                    """printf("%s" "\n",(true)?"true":"false");"""
                ),
                Arguments.of(
                    "a declared boolean emits the boolean typedef",
                    "let a: Boolean = true;",
                    """boolean_t $A=true;"""
                ),
                Arguments.of(
                    "printing a variable casts it to its declared type",
                    "let a = I64(42);\nprintln a;",
                    """int64_t $A=(int64_t)42;printf("%" PRId64 "\n",(int64_t)($A));"""
                ),
                Arguments.of(
                    "an expression statement discards its value",
                    "1 + 2;",
                    "(int32_t)((uint32_t)((int32_t)1)+(uint32_t)((int32_t)2));"
                ),
                Arguments.of(
                    "signed addition wraps by detouring through the unsigned counterpart",
                    "I32(1) + I32(2);",
                    "(int32_t)((uint32_t)((int32_t)1)+(uint32_t)((int32_t)2));"
                ),
                Arguments.of(
                    "signed subtraction takes the same detour",
                    "I32(1) - I32(2);",
                    "(int32_t)((uint32_t)((int32_t)1)-(uint32_t)((int32_t)2));"
                ),
                Arguments.of(
                    "signed multiplication takes the same detour",
                    "I8(3) * I8(4);",
                    "(int8_t)((uint32_t)((int8_t)3)*(uint32_t)((int8_t)4));"
                ),
                Arguments.of(
                    "a narrow detour widens to uint32_t, which c will not promote back to signed",
                    "I16(-1) * I16(-1);",
                    "(int16_t)((uint32_t)((int16_t)-1)*(uint32_t)((int16_t)-1));"
                ),
                Arguments.of(
                    "the widest signed type detours through the widest unsigned one",
                    "I64(3) * I64(4);",
                    "(int64_t)((uint64_t)((int64_t)3)*(uint64_t)((int64_t)4));"
                ),
                Arguments.of(
                    "the most negative i64 is emitted so that c never sees an out-of-range constant",
                    "I64(-9223372036854775808);",
                    "(int64_t)(-9223372036854775807-1);"
                ),
                Arguments.of(
                    "signed division does not detour, because MIN / -1 would change answer not width",
                    "I32(6) / I32(2);",
                    "(int32_t)((int32_t)6/(int32_t)2);"
                ),
                Arguments.of(
                    "signed remainder does not detour either",
                    "I32(7) % I32(2);",
                    "(int32_t)((int32_t)7%(int32_t)2);"
                ),
                Arguments.of(
                    "unsigned arithmetic needs no detour, only the result cast",
                    "U32(1) + U32(2);",
                    "(uint32_t)((uint32_t)1u+(uint32_t)2u);"
                ),
                Arguments.of(
                    "float arithmetic takes the result cast and no detour",
                    "F64(1.5) + F64(2.5);",
                    "(float64_t)((float64_t)1.5+(float64_t)2.5);"
                ),
                Arguments.of(
                    "unary minus on a signed type detours",
                    "-I32(5);",
                    "(int32_t)(-(uint32_t)((int32_t)5));"
                ),
                Arguments.of(
                    "unary plus takes the result cast without a detour",
                    "+I32(5);",
                    "(int32_t)(+(int32_t)5);"
                ),
                Arguments.of(
                    "a comparison yields a boolean and is left alone",
                    "I32(1) < I32(2);",
                    "((int32_t)1<(int32_t)2);"
                ),
                Arguments.of(
                    "negating a boolean is left alone",
                    "!true;",
                    "(!true);"
                ),
                Arguments.of(
                    "nested arithmetic composes the casts",
                    "(I32(1) + I32(2)) * I32(3);",
                    "(int32_t)((uint32_t)((int32_t)((uint32_t)((int32_t)1)+(uint32_t)((int32_t)2)))" +
                            "*(uint32_t)((int32_t)3));"
                )
            )
        }

        private const val A = "re_a"

        private fun String.mainBody(): String = substringAfter("int main(){")
            .substringBeforeLast(";return 0;}")
            .replace(Regex("re_a_[0-9a-f_]+"), A)
    }
}
