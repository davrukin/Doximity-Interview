plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

tasks.register<Exec>("installGitHooks") {
    group = "Build Setup"
    description = "Installs the pre-commit git hook by configuring core.hooksPath"
    commandLine("git", "config", "core.hooksPath", "scripts")
    isIgnoreExitValue = true
}

subprojects {
    afterEvaluate {
        tasks.configureEach {
            if (name == "preBuild") {
                dependsOn(":installGitHooks")
            }
        }
    }
}
