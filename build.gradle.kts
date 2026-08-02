plugins {
    base
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21" apply false
    id("com.gradleup.shadow") version "9.6.0" apply false
}

allprojects {
    group = "net.opmasterleo"
    version = "1.0.0"

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/groups/public/")
    }
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}
