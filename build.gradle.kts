import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    kotlin("jvm") version "2.4.10"
    id("maven-publish")
}

val modVersion = providers.gradleProperty("mod_version").get()
version = modVersion
group = project.property("maven_group") as String

base {
    archivesName = project.property("archives_base_name") as String
}

// ============================================================
// From Minecraft 26.1+ Minecraft is no longer obfuscated and Loom
// runs in unobfuscated mode, so no mappings dependency is needed.
// Same approach as TaCZ_Refabricated_Unofficial's 26.1.2 branch.
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

    // YACL 3.9.6+26.1-fabric (Modrinth version svTkvBec) supports 26.1.2.
    // Resolve it from Maven instead of requiring every source checkout to supply libs/yacl-fabric.jar.
    implementation("maven.modrinth:1eAoo2KR:svTkvBec")

    // The TaCZ refabricated port we integrate with (compile only in production; tests
    // exercise its codecs and therefore need it on their runtime classpath as well).
    val taczJar = files("libs/TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar")
    compileOnly(taczJar)
    testRuntimeOnly(taczJar)

    // MixinExtras (runtime bundled into our jar; AP used for compile-time validation)
    val mixinExtrasVersion = project.property("mixinextras_version") as String
    implementation("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")
    annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")
    include("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")

    // ModMenu (optional config screen entry)
    compileOnly("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")

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
    inputs.property("version", modVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

val mainSourceSet = sourceSets.named("main")
val testSourceSet = sourceSets.named("test")
// Gradle's Windows test-worker args file corrupts classpath entries when a project path
// contains characters not representable in the worker's native encoding (Gradle #30391).
// Stage every runtime entry under ASCII-only GRADLE_USER_HOME before forking the worker.
val testRuntimeKey = rootDir.absolutePath.hashCode().toUInt().toString(16)
val stagedTestRuntimeDir = gradle.gradleUserHomeDir.resolve(
    "caches/tacztweaks-test-runtime/$testRuntimeKey"
)
val stageTestRuntime by tasks.registering(org.gradle.api.tasks.Sync::class) {
    dependsOn("classes", "testClasses")
    into(stagedTestRuntimeDir)
    from(mainSourceSet.map { it.output }) { into("main") }
    from(testSourceSet.map { it.output }) { into("test") }
    from(configurations.named("testRuntimeClasspath")) { into("lib") }
}

tasks.named<org.gradle.api.tasks.testing.Test>("test") {
    dependsOn(stageTestRuntime)
    useJUnitPlatform()

    val stagedMain = stagedTestRuntimeDir.resolve("main")
    val stagedTest = stagedTestRuntimeDir.resolve("test")
    val stagedLibraries = stagedTestRuntimeDir.resolve("lib")
    val stagedWorkingDirectory = stagedTestRuntimeDir.resolve("work")
    testClassesDirs = files(stagedTest)
    classpath = files(stagedTest, stagedMain) + fileTree(stagedLibraries) {
        include("*.jar")
    }
    workingDir(stagedWorkingDirectory)
    doFirst {
        stagedWorkingDirectory.mkdirs()
    }
}

val examplePackZip by tasks.registering(org.gradle.api.tasks.bundling.Zip::class) {
    group = "distribution"
    description = "Packages the reloadable TaCZ Tweaks example gun pack."
    from(layout.projectDirectory.dir("tacz-tweaks-example-pack")) {
        into("tacz-tweaks-example-pack")
    }
    archiveFileName.set("tacz-tweaks-example-pack-$modVersion.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

tasks.named("assemble") {
    dependsOn(examplePackZip)
}

java {
    // Do not silently use the JVM which happened to start an old Gradle daemon as javac.
    // The target Minecraft/TaCZ classes require Java 25, so select an installed JDK 25 toolchain.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}
