import org.gradle.api.plugins.JavaPluginExtension

val sharedOut = layout.buildDirectory.dir("generated-shared")
val versionSharedPackage = "net.opmasterleo.packetuxui.nms.${project.name}.shared"
val versionSharedPath = versionSharedPackage.replace('.', '/')
val era = project.findProperty("nmsEra")?.toString() ?: "modern"
val nmsVersion = project.findProperty("nmsVersion")?.toString() ?: project.name

/**
 * One [nms-shared] tree, keyed by content variant (not era folder copies).
 * Each era picks adapter/item/pipeline/classifier/menu/(limits) variant dirs.
 */
data class SharedParts(
    val adapter: String,
    val item: String,
    val pipeline: String,
    val classifier: String,
    val menu: String,
    val limits: String? = null,
    val sign: String,
)

fun signVariant(): String = when {
    era == "legacy8" || era == "legacy82" || era == "legacy" -> "legacy8"
    era == "legacy9" -> "legacy9"
    era == "legacy11" -> "legacy11"
    era == "legacy13" -> "legacy13"
    era == "legacy14" || era == "legacy15" || era == "legacy16" -> "legacy14"
    era == "mid17" -> "mid17"
    era == "mid" && nmsVersion.startsWith("v1_20") -> "mid20"
    era == "mid" -> "mid17"
    era.startsWith("modern") -> "modern"
    else -> throw GradleException("Unknown sign variant for nmsEra=$era nmsVersion=$nmsVersion")
}

val parts: SharedParts = when (era) {
    "legacy8" -> SharedParts("legacy", "legacy", "legacy8", "legacy8", "legacy8", sign = signVariant())
    "legacy82" -> SharedParts("legacy", "legacy", "legacy82", "legacy8", "legacy_r3", sign = signVariant())
    "legacy" -> SharedParts("legacy", "legacy", "legacy", "legacy8", "legacy_r3", sign = signVariant())
    "legacy9" -> SharedParts("legacy", "legacy", "legacy", "legacy9", "legacy9", sign = signVariant())
    "legacy11" -> SharedParts("legacy", "legacy", "legacy", "legacy9", "legacy11", sign = signVariant())
    "legacy13" -> SharedParts("legacy", "legacy", "legacy", "legacy13", "legacy11", sign = signVariant())
    "legacy14" -> SharedParts("legacy", "legacy", "legacy", "legacy13", "legacy14", sign = signVariant())
    "legacy15" -> SharedParts("legacy", "legacy", "legacy", "legacy13", "legacy15", sign = signVariant())
    "legacy16" -> SharedParts("legacy", "legacy", "legacy", "legacy13", "legacy16", sign = signVariant())
    "mid17" -> SharedParts("mid", "mid17", "mid17", "mid17", "mid17", sign = signVariant())
    "mid" -> SharedParts("mid", "mid", "mid", "mid", "mid", sign = signVariant())
    "modern" -> SharedParts("mid", "modern", "modern", "mid", "modern", sign = signVariant())
    "modern21_2" -> SharedParts("modern21", "modern", "modern", "mid", "modern21_2", "modern21", signVariant())
    "modern21_5" -> SharedParts("modern21", "modern21_5", "modern", "modern21_5", "modern21_5", "modern21", signVariant())
    "modern26" -> SharedParts("modern21", "modern21_5", "modern", "modern26", "modern26", "modern21", signVariant())
    else -> throw GradleException("Unknown nmsEra=$era")
}

val sharedVariantDirs: List<File> = buildList {
    val root = rootProject.file("nms-shared")
    add(File(root, "adapter/${parts.adapter}"))
    add(File(root, "item/${parts.item}"))
    add(File(root, "pipeline/${parts.pipeline}"))
    add(File(root, "classifier/${parts.classifier}"))
    add(File(root, "menu/${parts.menu}"))
    parts.limits?.let { add(File(root, "limits/$it")) }
    add(File(root, "sign/${parts.sign}"))
}

val prepareSharedSources = tasks.register("prepareSharedSources") {
    sharedVariantDirs.forEach { inputs.dir(it) }
    inputs.property("nmsVersion", nmsVersion)
    inputs.property("era", era)
    inputs.property(
        "parts",
        "${parts.adapter}/${parts.item}/${parts.pipeline}/${parts.classifier}/${parts.menu}/${parts.limits}/${parts.sign}"
    )
    outputs.dir(sharedOut)
    doLast {
        val outRoot = sharedOut.get().asFile
        outRoot.deleteRecursively()
        var copied = 0
        for (variantDir in sharedVariantDirs) {
            if (!variantDir.isDirectory) {
                throw GradleException("Missing shared variant: ${variantDir.absolutePath}")
            }
            variantDir.listFiles { f -> f.isFile && f.extension == "java" }?.forEach { file ->
                val dest = File(outRoot, "$versionSharedPath/${file.name}")
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
            throw GradleException("No shared Java sources copied for era=$era")
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
