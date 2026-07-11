plugins {
    // java-library + shadow + Paper API + MagicUtils modules for the active
    // target. Bukkit's API is stable across the whole 1.21.x .. 26.2 range.
    id("magicutils.consumer-bukkit")
}

base {
    archivesName = "doublelife-bukkit"
}

description = "DoubleLife standalone plugin"

magicutilsConsumer {
    implementation("magicutils-bukkit", "magicutils-config-yaml")
    devServer {
        // runServer binds here; 25565 collides with the local Velocity, so the
        // consumer writes this port into the generated server.properties for us.
        port = 25599
        onlineMode = false
        modrinth("luckperms") {
            paper("mc1.21" to "v5.5.17-bukkit", "mc26" to "v5.5.53-bukkit")
        }
        modrinth("placeholderapi") {
            paper("mc1.21" to "2.11.7")
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation("com.google.code.gson:gson:2.10.1")

    compileOnly("net.luckperms:api:5.4")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
}

tasks.processResources {
    val props = mapOf("version" to project.version, "description" to project.description)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier = "plain"
    from(project(":common").sourceSets["main"].output)
}

tasks.shadowJar {
    archiveClassifier = ""
    mergeServiceFiles()
    relocate("com.google.gson", "dev.ua.theroer.doublelife.libs.gson")
    // MagicUtils is shaded into this jar (embed mode). Its config-yaml pulls a
    // modern Jackson, but Paper's PluginRemapper injects an older jackson-core
    // (2.13.x) that shadows ours and breaks YAMLParser at enable time
    // (NoSuchMethodError createChildObjectContext). Relocating Jackson rewrites
    // both our and the shaded MagicUtils call sites onto our own copy, so the
    // plugin always uses a consistent Jackson regardless of Paper's.
    relocate("com.fasterxml.jackson", "dev.ua.theroer.doublelife.libs.jackson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
