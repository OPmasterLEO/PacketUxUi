plugins {
    java
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper") version "2.0.1"
}

apply(from = rootProject.file("gradle/nms-buckets.gradle.kts"))

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly(project(":API"))
    implementation(project(":packetuxui"))
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
