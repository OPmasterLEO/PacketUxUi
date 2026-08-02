plugins {
    `java-library`
    id("com.gradleup.shadow")
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":nms-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-transport:4.1.115.Final")
    compileOnly("io.netty:netty-handler:4.1.115.Final")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.compileJava {
    options.release.set(21)
}
