import org.gradle.api.plugins.JavaPluginExtension

val sharedOut = layout.buildDirectory.dir("generated-shared")
val versionSharedPackage = "net.opmasterleo.packetuxui.nms.${project.name}.shared"
val versionSharedPath = versionSharedPackage.replace('.', '/')
val era = project.findProperty("nmsEra")?.toString() ?: "modern"
val nmsVersion = project.findProperty("nmsVersion")?.toString() ?: project.name

/** Ordered source roots: later layers overwrite earlier files with the same name. */
val sharedLayers: List<String> = when (era) {
    "legacy8", "legacy82", "legacy", "legacy9", "legacy11",
    "legacy13", "legacy14", "legacy15", "legacy16" ->
        listOf("nms-shared-legacy-base", "nms-shared-$era")
    "mid17", "mid" ->
        listOf("nms-shared-mid-base", "nms-shared-$era")
    "modern" ->
        listOf(
            "nms-shared-mid-base",
            "nms-shared-modern-common",
            "nms-shared-modern-item",
            "nms-shared-modern"
        )
    "modern21_2" ->
        listOf(
            "nms-shared-modern21-base",
            "nms-shared-modern-common",
            "nms-shared-modern-item",
            "nms-shared-modern21_2"
        )
    "modern21_5", "modern26" ->
        listOf(
            "nms-shared-modern21-base",
            "nms-shared-modern-common",
            "nms-shared-modern21_5-item",
            "nms-shared-$era"
        )
    else -> listOf("nms-shared-modern")
}

val prepareSharedSources = tasks.register("prepareSharedSources") {
    val fromPath = "net/opmasterleo/packetuxui/nms/shared"
    val layerRoots = sharedLayers.map { rootProject.file("$it/src/main/java") }
    layerRoots.forEach { inputs.dir(it) }
    inputs.property("nmsVersion", nmsVersion)
    inputs.property("era", era)
    inputs.property("layers", sharedLayers.joinToString(","))
    outputs.dir(sharedOut)
    doLast {
        val outRoot = sharedOut.get().asFile
        outRoot.deleteRecursively()
        var copied = 0
        for (srcRoot in layerRoots) {
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
                if (era.startsWith("legacy")) {
                    text = text
                        .replace("net.minecraft.server.NMS", "net.minecraft.server.$nmsVersion")
                        .replace("org.bukkit.craftbukkit.NMS", "org.bukkit.craftbukkit.$nmsVersion")
                } else if (era.startsWith("mid")) {
                    text = text.replace(
                        "org.bukkit.craftbukkit.NMS",
                        "org.bukkit.craftbukkit.$nmsVersion"
                    )
                }
                dest.writeText(text)
                copied++
            }
        }
        if (copied == 0) {
            throw GradleException("No shared Java sources copied for era=$era layers=$sharedLayers")
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
