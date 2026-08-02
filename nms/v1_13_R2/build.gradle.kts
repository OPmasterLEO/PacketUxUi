plugins {
    java
}

extra["nmsEra"] = "legacy"
extra["nmsVersion"] = "v1_13_R2"

dependencies {
    compileOnly(project(":nms-api"))
    compileOnly("org.spigotmc:spigot:1.13.2-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.68.Final")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
}

apply(from = rootProject.file("nms/shared-sources.gradle.kts"))

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
tasks.compileJava { options.release.set(21) }
tasks.jar { archiveClassifier.set("") }