import org.gradle.api.UnknownDomainObjectException

plugins {
    kotlin("jvm")
}

// TaCZ Tweaks — single-loader Fabric 26.2 build.
// Adapted from the multi-loader Stonecutter build. Loader is fixed to "fabric".

val loader = "fabric"
version = "${prop("version")}+${prop("minecraft.version")}-$loader"

base {
    archivesName = prop("archives_base_name")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(prop("java.version"))
        vendor = JvmVendorSpec.JETBRAINS
    }
    withSourcesJar()
}

repositories {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    exclusiveContent {
        forRepository {
            maven("https://maven.bawnorton.com/releases")
        }
        filter {
            includeGroup("com.github.bawnorton.mixinsquared")
        }
    }
    exclusiveContent {
        forRepository {
            maven("https://repo.spongepowered.org/repository/maven-public")
        }
        filter {
            includeGroupAndSubgroups("org.spongepowered")
        }
    }
    mavenCentral()
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.terraformersmc.com")
    maven("https://maven.shedaniel.me")
    mavenLocal()
    flatDir {
        dirs(rootProject.file("libs"))
    }
}

sourceSets {
    main {
        val sourceSet = this
        for ((name, srcDir) in arrayOf(
            "java" to java,
            "kotlin" to kotlin,
            "resources" to resources
        )) {
            srcDir.srcDirs("src/${sourceSet.name}/$name-$loader")
        }
    }
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${archiveBaseName.get()}" }
    }
}

val resourceProperties = extensions.create<ResourcePropertiesExtension>("resourceProperties").apply {
    properties.putAll(mapOf(
        "pack_format" to "61", // Minecraft 26.2
        "java_version" to prop("java.version"),
        "minecraft_version" to prop("minecraft.version"),
        "version" to prop("version"),
        "mod_id" to mod("id"),
        "mod_name" to mod("name"),
        "yacl_version" to libs("yacl")
    ))
}

tasks.processResources {
    val properties = resourceProperties.properties.get()
    inputs.properties(properties)
    doFirst {
        filesMatching(listOf(
            "pack.mcmeta", "*.mixins.json", "fabric.mod.json"
        )) {
            expand(properties)
        }
    }
}
