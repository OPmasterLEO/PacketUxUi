plugins {
    base
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

// JitPack looks for root publishToMavenLocal; the fat library lives in :packetuxui.
tasks.register("publishToMavenLocal") {
    group = "publishing"
    description = "Publishes PacketUxUi fat jar via :packetuxui"
    dependsOn(":packetuxui:publishToMavenLocal")
}

tasks.register("printVersion") {
    doLast {
        println(version)
    }
}
