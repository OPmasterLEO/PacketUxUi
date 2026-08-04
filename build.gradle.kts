plugins {
    `java-library`
    id("com.gradleup.shadow")
    `maven-publish`
    id("io.papermc.paperweight.userdev") apply false
}

allprojects {
    group = "net.opmasterleo"
    version = "0.8"

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.codemc.io/repository/nms/")
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
            artifactId = "packetuxui"
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
        fun org.gradle.api.artifacts.repositories.MavenArtifactRepository.reposiliteAuth() {
            isAllowInsecureProtocol = true
            credentials {
                username = project.findProperty("reposilite.user") as String? ?: System.getenv("REPOSILITE_USER")
                password = project.findProperty("reposilite.token") as String? ?: System.getenv("REPOSILITE_TOKEN")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "ReposiliteReleases"
            url = uri("http://repo.mastersmp.net/releases")
            reposiliteAuth()
        }
        maven {
            name = "ReposiliteSnapshots"
            url = uri("http://repo.mastersmp.net/snapshots")
            reposiliteAuth()
        }
    }
}

val publishingSnapshot = gradle.startParameter.taskNames.any {
    it == "publishSnapshot" || it.endsWith(":publishSnapshot")
}

tasks.named("publishMavenPublicationToReposiliteReleasesRepository").configure {
    onlyIf { !publishingSnapshot }
}

tasks.named("publishMavenPublicationToReposiliteSnapshotsRepository").configure {
    onlyIf { publishingSnapshot }
}

tasks.named("publishAllPublicationsToReposiliteReleasesRepository").configure {
    onlyIf { !publishingSnapshot }
}

tasks.named("publishAllPublicationsToReposiliteSnapshotsRepository").configure {
    onlyIf { publishingSnapshot }
}

tasks.named("publish").configure {
    group = "publishing"
    description = "Publish fat jar to Reposilite releases"
    dependsOn(tasks.shadowJar)
}

tasks.register("publishSnapshot") {
    group = "publishing"
    description = "Publish fat jar to Reposilite snapshots"
    dependsOn(tasks.shadowJar, "publishMavenPublicationToReposiliteSnapshotsRepository")
}

tasks.named("publishToMavenLocal").configure {
    dependsOn(tasks.shadowJar)
}

tasks.register("printVersion") {
    doLast {
        println(version)
    }
}
