import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom-remap")
    kotlin("jvm") version "2.4.10"
    id("maven-publish")
}

val modVersion = providers.gradleProperty("mod_version").get()
version = modVersion
group = project.property("maven_group") as String

base {
    archivesName = project.property("archives_base_name") as String
}

loom {
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

val compatStubClassesDir = layout.buildDirectory.dir("generated/compat-stubs/classes")
val compileCompatStubs by tasks.registering(JavaCompile::class) {
    source = fileTree("compat-stubs-src") { include("**/*.java") }
    classpath = files()
    destinationDirectory.set(compatStubClassesDir)
    options.encoding = "UTF-8"
    options.release.set(21)
}
val compatStubJar by tasks.registering(Jar::class) {
    archiveBaseName.set("tacztweaks-compat-stubs")
    archiveClassifier.set("compileonly")
    destinationDirectory.set(layout.buildDirectory.dir("generated/compat-stubs"))
    from(compatStubClassesDir)
    dependsOn(compileCompatStubs)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("flk_version")}")

    modImplementation(files("libs/yacl-fabric.jar"))

    val taczJar = files("libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar")
    modCompileOnly(taczJar)
    testRuntimeOnly(taczJar)

    compileOnly(files(compatStubJar.flatMap { it.archiveFile }))

    val mixinExtrasVersion = project.property("mixinextras_version") as String
    implementation("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")
    annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")
    include("io.github.llamalad7:mixinextras-fabric:$mixinExtrasVersion")

    modCompileOnly("com.terraformersmc:modmenu:${project.property("modmenu_version")}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.apache.commons:commons-math3:3.6.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

tasks.withType<JavaCompile>().configureEach {
    if (name != "compileCompatStubs") dependsOn(compatStubJar)
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-AdisableTargetValidator=true")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
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

val checkModIcon by tasks.registering(org.gradle.api.tasks.Exec::class) {
    group = "verification"
    description = "Verifies the mod icon checksum, metadata path, dimensions, and license notice."

    val checker = layout.projectDirectory.file("scripts/check_mod_icon.py")
    inputs.file(checker)
    inputs.file(layout.projectDirectory.file("src/main/resources/fabric.mod.json"))
    inputs.file(layout.projectDirectory.file("src/main/resources/icon.png"))
    inputs.file(layout.projectDirectory.file("THIRD_PARTY_NOTICES.md"))

    commandLine(
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "python" else "python3",
        checker.asFile.absolutePath,
    )
}

tasks.named("check") {
    dependsOn(checkModIcon)
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
    withSourcesJar()
}
