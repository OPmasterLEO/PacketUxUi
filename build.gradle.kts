plugins {
    `java-library`
    id("com.gradleup.shadow")
    `maven-publish`
    id("io.papermc.paperweight.userdev") apply false
}

allprojects {
    group = "net.opmasterleo"
    version = "1.0.0"

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/nms/")
    }
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    extensions.configure<JavaPluginExtension>("java") {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

apply(from = rootProject.file("gradle/nms-buckets.gradle.kts"))

@Suppress("UNCHECKED_CAST")
val nmsBuckets = extra["nmsBuckets"] as List<String>

dependencies {
    implementation(project(":API"))
    implementation(project(":nms-api"))
    nmsBuckets.forEach { bucket ->
        implementation(project(":nms:$bucket"))
    }
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = rootProject.name
            artifact(tasks.shadowJar) {
                classifier = null
            }
            pom {
                name.set("PacketUxUi")
                description.set(
                    "Virtual packet menus for Paper/Spigot/Folia (1.8–26.x). " +
                        "Fat jar with API + all NMS adapters shaded."
                )
                url.set("https://github.com/OPmasterLEO/PacketUxUi")
                licenses {
                    license {
                        name.set("See LICENSE.md")
                        url.set("https://github.com/OPmasterLEO/PacketUxUi/blob/main/LICENSE.md")
                    }
                }
                developers {
                    developer {
                        id.set("opmasterleo")
                        name.set("OPmasterLEO")
                    }
                }
                withXml {
                    asNode().appendNode("dependencies")
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

tasks.named("publishToMavenLocal").configure {
    dependsOn(tasks.shadowJar)
}

tasks.register("printVersion") {
    doLast {
        println(version)
    }
}
