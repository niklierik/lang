import arrow.core.getOrElse
import me.eriknikli.rhenium.common.diagnostics.render
import org.antlr.v4.runtime.CharStreams
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.fail

class SemanticAnalyzerTests {
    private val component = DaggerSemanticAnalyzerTestComponent.create()
    private val astBuilder = component.makeAstBuilder()
    private val semanticAnalyzer = component.makeSemanticAnalyzer()

    @ParameterizedTest(name = "Run {index}, name {0}")
    @MethodSource("provideData")
    fun `test semantic diagnostics`(name: String, sourceCode: String, expectedDiagnostics: String) {
        val ast = astBuilder.parse(CharStreams.fromString(sourceCode))
            .getOrElse { fail("expected the source to parse, got:\n${it.render()}") }

        val actualDiagnostics = semanticAnalyzer.decorateSemanticContext(ast)
            .fold({ it.render() }, { "" })

        assertEquals(expectedDiagnostics, actualDiagnostics)
    }

    companion object {
        @JvmStatic
        fun provideData(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("a valid program has no diagnostics", "let a = 1;\na = 2;", ""),
                Arguments.of("an expression statement is not a diagnostic", "let a = 1;\na + 1;", ""),
                Arguments.of(
                    "a broken expression statement reports once",
                    "b + 1;",
                    "1:1: unknown symbol 'b'."
                ),
                Arguments.of("unknown symbol", "b = 3;", "1:1: unknown symbol 'b'."),
                Arguments.of(
                    "assignment to an immutable variable",
                    "const a = 1;\na = 2;",
                    "2:1: cannot assign to 'a', it is not mutable. Declare it with 'let'."
                ),
                Arguments.of(
                    "declared type does not accept the value",
                    "let c: F32 = 1;",
                    "1:1: type mismatch, found I32 but expected F32."
                ),
                Arguments.of(
                    "unknown declared type",
                    "let a: Foo = 1;",
                    "1:8: unknown type 'Foo'."
                ),
                Arguments.of(
                    "redeclaration points at the first declaration",
                    "let a = 1;\nlet a = 9;",
                    "2:1: variable 'a' is already declared at 1:1."
                ),
                Arguments.of(
                    "boolean operand of an arithmetic operator",
                    "let a = true + 1;",
                    "1:9: illegal binary operation 'Boolean + I32'."
                ),
                Arguments.of(
                    "negating a boolean",
                    "let a = -true;",
                    "1:10: operator '-' cannot be applied to Boolean, expected " +
                            "I64 or I32 or I16 or I8 or F64 or F32."
                ),
                Arguments.of(
                    "one unknown symbol yields one diagnostic, not a cascade",
                    "let a = unknown + 1;",
                    "1:9: unknown symbol 'unknown'."
                ),
                Arguments.of(
                    "a poisoned declaration does not make later uses fail again",
                    "let a: Foo = 1;\na = 2;",
                    "1:8: unknown type 'Foo'."
                ),
                Arguments.of(
                    "both operands are reported",
                    "let a = left + right;",
                    """
                    1:9: unknown symbol 'left'.
                    1:16: unknown symbol 'right'.
                    """.trimIndent()
                ),
                Arguments.of(
                    "signed and unsigned operands cannot be mixed",
                    "let a = I32(1) + U32(1);",
                    "1:9: cannot mix signed and unsigned operands in 'I32 + U32'."
                ),
                Arguments.of(
                    "the operand order does not change the diagnostic",
                    "let a = U32(1) + I32(1);",
                    "1:9: cannot mix signed and unsigned operands in 'U32 + I32'."
                ),
                Arguments.of(
                    "mixing an integer with a float still resolves",
                    "let a: F64 = I32(1) + F64(1.0);",
                    ""
                ),
                Arguments.of(
                    "a float has no remainder operator",
                    "let a = F64(1.0) % F64(2.0);",
                    "1:9: illegal binary operation 'F64 % F64'."
                ),
                Arguments.of(
                    "the narrower float has no remainder operator either",
                    "let a = F32(1.0) % F32(2.0);",
                    "1:9: illegal binary operation 'F32 % F32'."
                ),
                Arguments.of(
                    "one float operand is enough to rule out a remainder",
                    "let a = I32(1) % F64(2.0);",
                    "1:9: illegal binary operation 'I32 % F64'."
                ),
                Arguments.of(
                    "integer remainder still resolves",
                    "let a: I32 = I32(7) % I32(2);",
                    ""
                ),
                Arguments.of(
                    "every statement is analysed, and diagnostics come out in source order",
                    "const a = 1;\na = 2;\nb = 3;\nlet c: F32 = 1;\nlet a = 9;",
                    """
                    2:1: cannot assign to 'a', it is not mutable. Declare it with 'let'.
                    3:1: unknown symbol 'b'.
                    4:1: type mismatch, found I32 but expected F32.
                    5:1: variable 'a' is already declared at 1:1.
                    """.trimIndent()
                )
            )
        }
    }
}
