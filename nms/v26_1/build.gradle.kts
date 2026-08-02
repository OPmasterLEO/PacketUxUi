import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
    id("io.papermc.paperweight.userdev")
}

extra["nmsEra"] = "modern21_5"

dependencies {
    compileOnly(project(":nms-api"))
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
    paperweight.paperDevBundle("26.1.2.build.+")
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

paperweight.javaLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

apply(from = rootProject.file("nms/shared-sources.gradle.kts"))

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}
tasks.compileJava {
    options.compilerArgs.addAll(listOf("--release", "21"))
}
tasks.jar { archiveClassifier.set("") }
tasks.matching { it.name == "reobfJar" }.configureEach { enabled = false }
configurations.configureEach {
    if (name.contains("reobf", ignoreCase = true)) {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.named("jar"))
    }
    if (isCanBeResolved) {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}