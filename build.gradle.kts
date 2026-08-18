import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom-remap")
    kotlin("jvm") version "2.4.10"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName = project.property("archives_base_name") as String
}

loom {
    // 1.21.11 is obfuscated -> legacy mixin AP + refmap are REQUIRED so that vanilla
    // mixin targets written in Mojang names are translated to intermediary at runtime.
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = "tacztweaks.refmap.json"
    }
    runs {
        named("client") { vmArg("-Xmx1G") }
        named("server") { vmArg("-Xmx768m") }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.terraformersmc.com/releases/") }
    flatDir { dirs("libs") }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // Kotlin runtime (FLK is a remapped mod jar; Loom skips re-remapping it)
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("flk_version")}")

    // YACL — Fabric 1.21.11 build, provided as a hard dependency
    modImplementation(files("libs/yacl-fabric.jar"))

    // TaCZ refabricated port (compile only, mixin targets)
    modCompileOnly(files("libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar"))

    // MixinExtras (bundled into our jar)
    val mixinExtrasVersion = project.property("mixinextras_version") as String
    implementation("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")
    annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")
    include("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")

    modCompileOnly("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    compileOnly("org.apache.commons:commons-math3:3.6.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    withSourcesJar()
}
