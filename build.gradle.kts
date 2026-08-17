plugins {
    id("multiloader-common")
    id("fabric-loom") version "1.17-SNAPSHOT"
}

// TaCZ Tweaks — single-loader Fabric 26.2 build (adapted for TaCZ_Refabricated_Unofficial).
// Minecraft 26.2 is unobfuscated: no mappings are required.

loom {
    accessWidenerPath = rootProject.file("src/main/resources-fabric/${mod("id")}.accesswidener")

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

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft.version")}")
    // 26.2 is unobfuscated — no mappings needed.
    include(implementation(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:${libs("mixinsquared")}")) { }) { }
    modImplementation("net.fabricmc:fabric-loader:${libs("fabric.loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${libs("fabric.kotlin")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${libs("fabric.api")}")
    modImplementation("dev.isxander:yet-another-config-lib:${libs("yacl")}")

    // TaCZ (TaCZ_Refabricated_Unofficial 26.2) from local flatDir `libs/`.
    modImplementation("cn.sh1rocu:tacz-refabricated:${libs("tacz")}")
    implementation("com.maydaymemory:mae:1.1.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.github.FiguraMC.luaj:luaj-core:3.0.8-figura")
    implementation("com.github.FiguraMC.luaj:luaj-jse:3.0.8-figura")
    implementation("org.apache.bcel:bcel:6.6.1")

    // Compatibility
    modCompileOnly("com.terraformersmc:modmenu:${libs("modmenu")}")
    modCompileOnly("maven.modrinth:sound-physics-remastered:${libs("sound-physics-remastered")}")

    // Runtime
    modLocalRuntime("com.terraformersmc:modmenu:${libs("modmenu")}")
    modLocalRuntime("maven.modrinth:sound-physics-remastered:${libs("sound-physics-remastered")}")
}

resourceProperties {
    properties.putAll(mapOf(
        "loader_version" to libs("fabric.loader"),
        "fabric_api_version" to libs("fabric.api"),
        "kotlin_loader_version" to libs("fabric.kotlin"),
        "tacz_version_min" to prop("version.target.min")
    ))
}
