plugins {
    base
    `maven-publish`
    id("io.papermc.paperweight.userdev") apply false
    id("com.gradleup.shadow") apply false
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

evaluationDependsOn(":packetuxui")

val fatJar = project(":packetuxui").tasks.named("shadowJar")

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = rootProject.name // PacketUxUi
            artifact(fatJar) {
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
    dependsOn(fatJar)
}

tasks.register("printVersion") {
    doLast {
        println(version)
    }
}
