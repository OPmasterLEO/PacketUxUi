plugins {
    java
}

extra["nmsEra"] = "mid"

dependencies {
    compileOnly(project(":nms-api"))
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
    compileOnly("org.spigotmc:spigot:1.19.2-R0.1-SNAPSHOT:remapped-mojang")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("io.netty:netty-transport:4.1.118.Final")
    compileOnly("io.netty:netty-handler:4.1.118.Final")
}

apply(from = rootProject.file("nms/shared-sources.gradle.kts"))

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
tasks.compileJava { options.release.set(21) }
tasks.jar { archiveClassifier.set("") }