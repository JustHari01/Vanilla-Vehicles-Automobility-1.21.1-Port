@file:Suppress("UnstableApiUsage")

plugins {
    id("fabric-loom") version ("1.9-SNAPSHOT")
}

dependencies {
    minecraft("com.mojang:minecraft:${rootProject.properties["minecraft_version"]}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${rootProject.properties["minecraft_version"]}:${rootProject.properties["parchment_release"]}@zip")
    })

    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    compileOnly("net.fabricmc:sponge-mixin:0.15.3+mixin.0.8.7")
    modImplementation("net.fabricmc:fabric-loader:${rootProject.properties["fabric_version"]}")
}

loom {
    mixin {
        useLegacyMixinAp = false
    }

    accessWidenerPath = file("src/main/resources/automobility.accesswidener")
}

tasks {
    jar { enabled = false }
    remapJar { enabled = false }
}
