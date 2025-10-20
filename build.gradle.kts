// Arquivo: build.gradle.kts (raiz)
plugins {
    // O plugin Android não é aplicado aqui
    // Apenas ferramentas e configurações globais
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
