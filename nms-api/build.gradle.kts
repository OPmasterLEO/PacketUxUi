plugins {
    `java-library`
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    val paperApiVersion: String by project
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("io.netty:netty-transport:4.1.115.Final")
}
