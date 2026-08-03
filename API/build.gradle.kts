plugins {
    `java-library`
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":nms-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-transport:4.1.115.Final")
    compileOnly("io.netty:netty-handler:4.1.115.Final")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    options.release.set(21)
}

tasks.named<Javadoc>("javadoc") {
    options {
        (this as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            addBooleanOption("Xdoclint:none", true)
        }
    }
    isFailOnError = false
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.opmasterleo"
            artifactId = "packetuxui-api"
            version = project.version.toString()
            from(components["java"])
            pom {
                name.set("PacketUxUi API")
                description.set(
                    "Compile-only API for PacketUxUi. For runtime, depend on net.opmasterleo:packetuxui (shaded NMS)."
                )
                url.set("https://github.com/OPmasterLEO/PacketUxUi")
            }
        }
    }
    repositories {
        mavenLocal()
    }
}
