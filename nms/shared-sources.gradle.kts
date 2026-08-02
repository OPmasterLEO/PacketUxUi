import org.gradle.api.plugins.JavaPluginExtension

val sharedOut = layout.buildDirectory.dir("generated-shared")
val versionSharedPackage = "net.opmasterleo.packetuxui.nms.${project.name}.shared"
val versionSharedPath = versionSharedPackage.replace('.', '/')
val era = project.findProperty("nmsEra")?.toString() ?: "modern"
val nmsVersion = project.findProperty("nmsVersion")?.toString() ?: project.name
val sharedRootName = when (era) {
    "legacy" -> "nms-shared-legacy"
    "mid" -> "nms-shared-mid"
    "modern21_5" -> "nms-shared-modern21_5"
    else -> "nms-shared-modern"
}

val prepareSharedSources = tasks.register("prepareSharedSources") {
    val srcRoot = rootProject.file("$sharedRootName/src/main/java")
    val fromPath = "net/opmasterleo/packetuxui/nms/shared"
    inputs.dir(srcRoot)
    inputs.property("nmsVersion", nmsVersion)
    inputs.property("era", era)
    outputs.dir(sharedOut)
    doLast {
        val outRoot = sharedOut.get().asFile
        outRoot.deleteRecursively()
        val fromDir = File(srcRoot, fromPath)
        if (!fromDir.isDirectory) {
            throw GradleException("Missing shared sources: ${fromDir.absolutePath}")
        }
        fromDir.walkTopDown().filter { it.isFile && it.extension == "java" }.forEach { file ->
            val relative = file.relativeTo(fromDir)
            val dest = File(outRoot, "$versionSharedPath/$relative")
            dest.parentFile.mkdirs()
            var text = file.readText()
                .replace(
                    "package net.opmasterleo.packetuxui.nms.shared;",
                    "package $versionSharedPackage;"
                )
                .replace(
                    "import net.opmasterleo.packetuxui.nms.shared.",
                    "import $versionSharedPackage."
                )
            if (era == "legacy") {
                text = text
                    .replace("net.minecraft.server.NMS", "net.minecraft.server.$nmsVersion")
                    .replace("org.bukkit.craftbukkit.NMS", "org.bukkit.craftbukkit.$nmsVersion")
            }
            dest.writeText(text)
        }
    }
}

val sourceSets = extensions.getByType(JavaPluginExtension::class.java).sourceSets
sourceSets.named("main") {
    java {
        setSrcDirs(listOf(sharedOut, file("src/main/java")))
    }
}

tasks.named("compileJava") {
    dependsOn(prepareSharedSources)
}

tasks.named("jar") {
    dependsOn(prepareSharedSources)
}
