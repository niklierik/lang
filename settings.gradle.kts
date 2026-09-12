dependencyResolutionManagement {
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "rhenium"

include(":app")
include(":common")
include(":parser")
include(":ast")
include(":semanticAnalyzer")
include(":lowering")
include(":transpiler")
include(":semanticContext")
