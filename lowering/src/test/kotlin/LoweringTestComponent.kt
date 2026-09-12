import dagger.Component
import me.eriknikli.rhenium.ast.AstModule
import me.eriknikli.rhenium.ast.IAstBuilder
import me.eriknikli.rhenium.lowering.ILowerer
import me.eriknikli.rhenium.lowering.LoweringModule
import me.eriknikli.rhenium.semanticAnalyzer.ISemanticAnalyzer
import me.eriknikli.rhenium.semanticAnalyzer.SemanticAnalyzerModule
import javax.inject.Singleton

@Component(modules = [AstModule::class, SemanticAnalyzerModule::class, LoweringModule::class])
@Singleton
interface LoweringTestComponent {
    fun makeAstBuilder(): IAstBuilder
    fun makeSemanticAnalyzer(): ISemanticAnalyzer
    fun makeLowerer(): ILowerer
}
