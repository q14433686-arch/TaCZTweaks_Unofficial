// Buildscript for the 26.2-fabric stonecutter version only.
//
// TaCZ Tweaks adapted for the unofficial TaCZ Refabricated 26.2 port
// (q14433686-arch/TaCZ_Refabricated_Unofficial, `1.1.8+fabric.26.2.R2`).
//
// Key differences vs the shared fabric.gradle.kts (1.20.1/1.21.1):
//  - MC 26.1+ ships UNOBFUSCATED Minecraft: no mappings, no remap, so mods are plain `implementation`.
//  - Requires Loom 1.17+ (plain `net.fabricmc.fabric-loom`; the `fabric-loom-remap` fork is for
//    obfuscated versions only).
//  - tacz is not on any maven: it is fetched from GitHub Releases into libs/ (scripts/fetch-tacz-26.2.sh).
//  - Access widener is `v2 official` (src/main/resources-fabric-26.2/).
//  - pack.mcmeta is excluded (26.x doesn't ship/require one).
//
// See docs/PORT_PLAN_26.2.md for the full analysis.

plugins {
    id("multiloader-common")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources-fabric-26.2/${mod("id")}.accesswidener")

    runs {
        named("client") {
            programArgs("--username=Dev")
            runDir = "run/client"
        }

        create("client2") {
            client()
            programArgs("--username=Dev2")
            runDir = "run/clientB"
        }

        named("server") {
            programArgs("--nogui")
            runDir = "run/server"
        }

        configureEach {
            vmArgs("-XX:+AllowEnhancedClassRedefinition")
            isIdeConfigGenerated = true
        }
    }

    mods {
        create(mod("id")) {
            sourceSet(sourceSets["main"])
        }
    }
}

repositories {
    exclusiveContent {
        forRepository {
            maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven")
        }
        filter {
            includeGroup("fuzs.forgeconfigapiport")
        }
    }
}

// Optional-compat mods without 26.2 builds (see docs/PORT_PLAN_26.2.md §6.4):
// exclude their manager classes and mixin packages from compilation for 26.2.
sourceSets {
    main {
        java.exclude(
            "me/muksc/tacztweaks/feature/general/compatibility/sable/**",
            "me/muksc/tacztweaks/feature/general/compatibility/vs/**",
            "me/muksc/tacztweaks/feature/general/compatibility/lrtactical/**"
        )
        kotlin.exclude(
            "me/muksc/tacztweaks/feature/general/compatibility/**",
        )
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft.version")}")
    // 26.1+: unobfuscated Minecraft — no mappings, and mod jars need no remapping.
    implementation("net.fabricmc:fabric-loader:${libs("fabric.loader")}")
    implementation("net.fabricmc:fabric-language-kotlin:${libs("fabric.kotlin")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${libs("fabric.api")}")
    implementation("dev.isxander:yet-another-config-lib:${libs("yacl")}")

    // Unofficial TaCZ Refabricated 26.2 — published on GitHub Releases only (not Modrinth maven).
    val taczJar = rootProject.file("libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar")
    check(taczJar.exists()) { "Missing $taczJar — run: bash scripts/fetch-tacz-26.2.sh" }
    implementation(files(taczJar))
    // tacz hard-depends on Forge Config API Port 26.2.1; it also provides the ForgeConfigSpec class
    // that TaCZ Tweaks' mixin descriptors reference (e.g. SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER).
    runtimeOnly("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.2.1")

    include(implementation(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:${libs("mixinsquared")}")) { }) { }

    // Compatibility
    compileOnly("maven.modrinth:modmenu:${libs("modmenu")}")
    compileOnly("maven.modrinth:sound-physics-remastered:${libs("sound-physics-remastered")}")

    // Runtime
    runtimeOnly("maven.modrinth:modmenu:${libs("modmenu")}")
    runtimeOnly("maven.modrinth:sound-physics-remastered:${libs("sound-physics-remastered")}")
}

resourceProperties {
    properties.putAll(mapOf(
        "loader_version" to libs("fabric.loader"),
        "fabric_api_version" to libs("fabric.api"),
        "kotlin_loader_version" to libs("fabric.kotlin"),
        "tacz_version_min" to prop("version.target.min")
    ))
}

tasks.processResources {
    // 26.x doesn't ship pack.mcmeta (the upstream 26.2 port has none); exclude the stale template
    // rather than emit an unknown pack_format number.
    exclude("pack.mcmeta")
}

publishMods {
    file = tasks.jar.get().archiveFile
    additionalFiles.from(tasks.sourcesJar.get().archiveFile)
}
