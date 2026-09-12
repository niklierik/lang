package me.eriknikli.rhenium.app

import dagger.Component
import me.eriknikli.rhenium.ast.AstModule
import me.eriknikli.rhenium.lowering.LoweringModule
import me.eriknikli.rhenium.semanticAnalyzer.SemanticAnalyzerModule
import me.eriknikli.rhenium.transpiler.CTranspilerModule
import javax.inject.Singleton

@Component(
    modules = [
        AstModule::class,
        AppModule::class,
        SemanticAnalyzerModule::class,
        LoweringModule::class,
        CTranspilerModule::class,
    ]
)
@Singleton
interface RheniumCompilerFactory {
    fun makeCompiler(): IRheniumCompiler
}