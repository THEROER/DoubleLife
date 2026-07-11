plugins {
    base
    // Matrix/smoke/release tasks (listBuildMatrix, runCompatibilitySmoke,
    // release, ...) from MagicUtils build-logic, driven by magicMatrix { } in
    // settings.gradle. The per-module MagicUtils wiring (Paper API, classifier,
    // toolchain) is handled by the magicutils.consumer-* plugins.
    id("magicutils.matrix-root")
}

allprojects {
    group = (project.findProperty("group") ?: "dev.ua.theroer") as String
    version = (project.findProperty("version") ?: "1.0.1") as String

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.theroer.dev/releases") }
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

// Root run tasks (runPaper / runFolia) are provided by magicutils.matrix-root
// for the bukkit module which opted into `magicutilsConsumer { devServer { } }`.
