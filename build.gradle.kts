import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    kotlin("jvm") version "2.4.10"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName = project.property("archives_base_name") as String
}

// ============================================================
// From Minecraft 26.1+ Minecraft is no longer obfuscated and Loom
// runs in unobfuscated mode, so no mappings dependency is needed.
// Same approach as TaCZ_Refabricated_Unofficial's 26.2 branch.
// ============================================================

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.terraformersmc.com/releases/") } // modmenu
    maven { url = uri("https://api.modrinth.com/maven") }
    flatDir { dirs("libs") }
}

dependencies {
    // Minecraft & Fabric
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // Kotlin runtime is provided by Fabric Language Kotlin at runtime;
    // putting it on the compile classpath keeps stdlib versions aligned.
    implementation("net.fabricmc:fabric-language-kotlin:${project.property("flk_version")}")

    // YACL (YetAnotherConfigLib) — Fabric 26.2 build, provided as a hard dependency
    implementation(files("libs/yacl-fabric.jar"))

    // The TaCZ refabricated port we integrate with (compile only)
    compileOnly(files("libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar"))

    // MixinExtras (runtime bundled into our jar; AP used for compile-time validation)
    val mixinExtrasVersion = project.property("mixinextras_version") as String
    implementation("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")
    annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")
    include("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")

    // ModMenu (optional config screen entry)
    compileOnly("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // commons-math3 is bundled inside the TaCZ jar; we only need it at compile time
    compileOnly("org.apache.commons:commons-math3:3.6.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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
