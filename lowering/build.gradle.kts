plugins {
    id("buildsrc.convention.kotlin-jvm")
}

group = "me.eriknikli"

val rheniumVersion: String by project

version = rheniumVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ast"))
    implementation(project(":semanticContext"))
    testImplementation(project(":common"))
    testImplementation(project(":parser"))
    testImplementation(project(":semanticAnalyzer"))
}

tasks.test {
    useJUnitPlatform()
    dependsOn(":parser:generateGrammarSource")
}
kotlin {
    jvmToolchain(25)
}
