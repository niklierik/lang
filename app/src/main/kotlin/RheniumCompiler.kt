package me.eriknikli.rhenium.app

import arrow.core.raise.either
import me.eriknikli.rhenium.ast.IAstBuilder
import me.eriknikli.rhenium.common.diagnostics.Diagnosed
import me.eriknikli.rhenium.common.runCommand
import me.eriknikli.rhenium.lowering.ILowerer
import me.eriknikli.rhenium.semanticAnalyzer.ISemanticAnalyzer
import me.eriknikli.rhenium.transpiler.ITranspiler
import org.antlr.v4.runtime.CharStreams
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.Path
import kotlin.io.path.outputStream

interface IRheniumCompiler {
    fun compile(options: CompilerOptions): Diagnosed<Unit>
}

@Singleton
class RheniumCompiler
@Inject
constructor(
    private val astBuilder: IAstBuilder,
    private val semanticAnalyzer: ISemanticAnalyzer,
    private val lowerer: ILowerer,
    private val transpiler: ITranspiler
) : IRheniumCompiler {
    override fun compile(options: CompilerOptions): Diagnosed<Unit> = either {
        val stream = CharStreams.fromFileName(options.inputPath)

        val ast = astBuilder.parse(stream).bind()
        semanticAnalyzer.decorateSemanticContext(ast).bind()

        val actions = lowerer.lower(ast)

        val output = Path("${options.inputPath}.c").outputStream()
        output.use {
            transpiler.transpile(actions, it)
        }

        val binaryPath = File("${options.inputPath}.o").absolutePath

        "clang ${options.inputPath}.c -o $binaryPath -lm".runCommand()
        binaryPath.runCommand()
    }
}
