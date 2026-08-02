import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    java
    id("io.papermc.paperweight.userdev")
}

extra["nmsEra"] = "modern21_5"

dependencies {
    compileOnly(project(":nms-api"))
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-transport:4.1.118.Final")
    compileOnly("io.netty:netty-handler:4.1.118.Final")
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

apply(from = rootProject.file("nms/shared-sources.gradle.kts"))

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
tasks.compileJava { options.release.set(21) }
tasks.jar { archiveClassifier.set("") }
tasks.matching { it.name == "reobfJar" }.configureEach { enabled = false }