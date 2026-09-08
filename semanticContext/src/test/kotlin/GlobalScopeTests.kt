import me.eriknikli.rhenium.semanticContext.scope.globalScope
import me.eriknikli.rhenium.semanticContext.scope.types.BooleanType
import me.eriknikli.rhenium.semanticContext.scope.types.FloatType
import me.eriknikli.rhenium.semanticContext.scope.types.SignedIntType
import me.eriknikli.rhenium.semanticContext.scope.types.UnsignedIntType
import me.eriknikli.rhenium.semanticContext.scope.Symbol
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class GlobalScopeTests {
    @ParameterizedTest(name = "Run {index}, name {0}")
    @MethodSource("provideData")
    fun `test a primitive name resolves to the type it spells`(name: String, expected: Symbol) {
        assertEquals(expected, globalScope().getSymbol(name))
    }

    companion object {
        @JvmStatic
        fun provideData(): Stream<Arguments> = Stream.of(
            Arguments.of("I8", SignedIntType.I8),
            Arguments.of("I16", SignedIntType.I16),
            Arguments.of("I32", SignedIntType.I32),
            Arguments.of("I64", SignedIntType.I64),
            Arguments.of("U8", UnsignedIntType.U8),
            Arguments.of("U16", UnsignedIntType.U16),
            Arguments.of("U32", UnsignedIntType.U32),
            Arguments.of("U64", UnsignedIntType.U64),
            Arguments.of("F32", FloatType.F32),
            Arguments.of("F64", FloatType.F64),
            Arguments.of("Boolean", BooleanType)
        )
    }
}
