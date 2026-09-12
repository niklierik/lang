package me.eriknikli.rhenium.transpiler

import dagger.Binds
import dagger.Module

@Module
interface CTranspilerModule {
    @Binds
    fun bindTranspiler(cTranspiler: CTranspiler): ITranspiler
}
