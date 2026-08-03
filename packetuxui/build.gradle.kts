plugins {
    `java-library`
    id("com.gradleup.shadow")
}

apply(from = rootProject.file("gradle/nms-buckets.gradle.kts"))

@Suppress("UNCHECKED_CAST")
val nmsBuckets = extra["nmsBuckets"] as List<String>

dependencies {
    // Shaded into the published library artifact
    implementation(project(":API"))
    implementation(project(":nms-api"))
    nmsBuckets.forEach { bucket ->
        implementation(project(":nms:$bucket"))
    }

    // Provided by the consuming plugin / server
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.jar {
    enabled = false
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveBaseName.set("PacketUxUi")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/maven/**"
    )
    mergeServiceFiles()
    manifest {
        attributes["Automatic-Module-Name"] = "net.opmasterleo.packetuxui"
        attributes["Implementation-Title"] = "PacketUxUi"
        attributes["Implementation-Version"] = project.version
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

// Expose the fat jar to project dependents (TestMenu, etc.)
listOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name).configure {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.shadowJar)
    }
}

tasks.compileJava {
    options.release.set(21)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
