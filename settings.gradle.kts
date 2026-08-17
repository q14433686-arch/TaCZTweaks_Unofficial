pluginManagement {
    repositories {
        exclusiveContent {
            forRepository {
                maven("https://maven.fabricmc.net")
            }
            filter {
                includeGroupAndSubgroups("net.fabricmc")
                includeGroup("fabric-loom")
            }
        }
        maven("https://maven.kikugie.dev/snapshots") {
            content {
                includeGroupAndSubgroups("dev.kikugie")
            }
        }
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.5"
}

stonecutter {
    create(rootProject) {
        fun mc(mcVersion: String, loaders: Iterable<String>) =
            loaders.forEach { loader -> version(project = "$mcVersion-$loader", version = mcVersion).buildscript("$loader.gradle.kts") }

        mc("1.20.1", listOf("fabric", "forge"))
        mc("1.21.1", listOf("fabric", "neoforge"))
        // Unofficial TaCZ Refabricated port: https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial
        // 26.2 needs plain fabric-loom 1.17 (unobfuscated pipeline), so it gets its own buildscript.
        // (see docs/PORT_PLAN_26.2.md)
        version(project = "26.2-fabric", version = "26.2").buildscript("26.2-fabric.gradle.kts")
        vcsVersion = "1.20.1-forge"
    }
}

rootProject.name = "tacz-tweaks"