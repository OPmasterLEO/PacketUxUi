plugins {
    java
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper") version "2.0.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val nmsBuckets = listOf(
    "v1_8_R1", "v1_8_R2", "v1_8_R3",
    "v1_9_R1", "v1_9_R2",
    "v1_10_R1",
    "v1_11_R1",
    "v1_12_R1",
    "v1_13_R1", "v1_13_R2",
    "v1_14_R1",
    "v1_15_R1",
    "v1_16_R1", "v1_16_R2", "v1_16_R3",
    "v1_17_R1",
    "v1_18_R1", "v1_18_R2",
    "v1_19_R1", "v1_19_R2", "v1_19_R3",
    "v1_20_R1", "v1_20_R2", "v1_20_R3", "v1_20_R4",
    "v1_21_R1", "v1_21_R2", "v1_21_R3", "v1_21_R4", "v1_21_R5", "v1_21_R6", "v1_21_R7",
    "v26_1", "v26_2"
)

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation(project(":API"))
    implementation(project(":nms-api"))

    nmsBuckets.forEach { bucket ->
        implementation(project(":nms:$bucket"))
    }

    implementation("org.incendo:cloud-paper:2.0.0-beta.10")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/maven/**"
    )
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.runServer {
    minecraftVersion("1.21.4")
}

tasks.compileJava {
    options.release.set(21)
}
