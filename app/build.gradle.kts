plugins {
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":common"))
    implementation(project(":parser"))
    implementation(project(":ast"))
    implementation(project(":semanticAnalyzer"))
    implementation(project(":lowering"))
    implementation(project(":semanticContext"))
    implementation(project(":transpiler"))
    implementation("io.github.joelromanpr:commandline-ktx:1.0.0")
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "me.eriknikli.rhenium.app.MainKt"
}


val rheniumVersion: String by project

version = rheniumVersion

group = "me.eriknikli"