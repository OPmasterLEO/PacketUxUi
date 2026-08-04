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
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-transport:4.1.115.Final")
}
