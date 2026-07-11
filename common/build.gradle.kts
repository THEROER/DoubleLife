plugins {
    // Applies java-library + the active MagicUtils target (toolchain, Java) and
    // resolves the declared MagicUtils modules with the right classifier.
    id("magicutils.consumer-common")
}

magicutilsConsumer {
    api("magicutils-config", "magicutils-lang")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
}
