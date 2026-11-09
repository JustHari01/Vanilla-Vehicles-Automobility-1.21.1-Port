rootProject.name = "Automobility"

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")

        mavenCentral()
        gradlePluginPortal()
    }
}

// This distribution is intentionally NeoForge-only.  Keeping the Fabric
// project out of the settings graph avoids resolving Fabric-only controller
// dependencies on a dedicated NeoForge build.
include("common", "neoforge")
