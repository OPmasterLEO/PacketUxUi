pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "PacketUxUi"

include("nms-api")
include("API")
include("TestMenu")

val nmsBuckets = listOf(
    "v1_8_R1", "v1_8_R2", "v1_8_R3",
    "v1_9_R1", "v1_9_R2",
    "v1_10_R1",
    "v1_11_R1",
    "v1_12_R1",
    "v1_13_R1", "v1_13_R2",
    "v1_14_R1",
    "v1_15_R1",
    "v1_16_R1", "v1_16_R2", "v1_16_R3",
    "v1_17_R1",
    "v1_18_R1", "v1_18_R2",
    "v1_19_R1", "v1_19_R2", "v1_19_R3",
    "v1_20_R1", "v1_20_R2", "v1_20_R3", "v1_20_R4",
    "v1_21_R1", "v1_21_R2", "v1_21_R3", "v1_21_R4", "v1_21_R5", "v1_21_R6", "v1_21_R7",
    "v26_1", "v26_2"
)

nmsBuckets.forEach { bucket ->
    include("nms:$bucket")
    project(":nms:$bucket").projectDir = file("nms/$bucket")
}
