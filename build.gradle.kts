import java.security.MessageDigest
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

val checkModIcon by tasks.registering {
    group = "verification"
    description = "Verifies the mod icon checksum, metadata path, dimensions, and license notice."
    val checker = layout.projectDirectory.file("scripts/check_mod_icon.py")
    val metadataFile = layout.projectDirectory.file("src/main/resources/fabric.mod.json")
    val iconFile = layout.projectDirectory.file("src/main/resources/icon.png")
    val noticeFile = layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")

    inputs.file(checker)
    inputs.file(metadataFile)
    inputs.file(iconFile)
    inputs.file(noticeFile)

    doLast {
        val osName = System.getProperty("os.name") ?: ""
        val isWindows = osName.startsWith("Windows", ignoreCase = true)
        val pythonCommands = if (isWindows) listOf("py", "python", "python3") else listOf("python3", "python")

        var execSuccess = false
        for (cmd in pythonCommands) {
            try {
                val process = ProcessBuilder(cmd, checker.asFile.absolutePath)
                    .directory(rootDir)
                    .inheritIO()
                    .start()
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    execSuccess = true
                    break
                }
            } catch (_: Exception) {
                // Command not found or failed to execute
            }
        }

        if (!execSuccess) {
            val errors = mutableListOf<String>()

            val metaPath = metadataFile.asFile
            if (!metaPath.exists()) {
                errors.add("cannot read ${metaPath.name}")
            } else {
                val metaText = metaPath.readText(Charsets.UTF_8)
                if (!metaText.contains("\"icon\": \"icon.png\"") && !metaText.contains("\"icon\": 'icon.png'")) {
                    errors.add("fabric.mod.json icon must be 'icon.png'")
                }
            }

            val iconPath = iconFile.asFile
            if (!iconPath.exists()) {
                errors.add("cannot read ${iconPath.name}")
            } else {
                val bytes = iconPath.readBytes()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes).joinToString("") { b -> "%02x".format(b) }
                val expectedSha = "c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d"
                val rejectedSha = "5e1272a625af1b0b4d866d0fb468e1cea0a9258411f16a7d06d31a84e8953ac8"

                if (digest == rejectedSha) {
                    errors.add("mod icon is the known dark-grey/orange placeholder")
                } else if (digest != expectedSha) {
                    errors.add("mod icon SHA-256 drifted: expected $expectedSha, got $digest")
                }

                if (bytes.size < 24 ||
                    bytes[0] != 0x89.toByte() || bytes[1] != 'P'.code.toByte() ||
                    bytes[2] != 'N'.code.toByte() || bytes[3] != 'G'.code.toByte() ||
                    bytes[12] != 'I'.code.toByte() || bytes[13] != 'H'.code.toByte() ||
                    bytes[14] != 'D'.code.toByte() || bytes[15] != 'R'.code.toByte()
                ) {
                    errors.add("mod icon is not a PNG with a valid leading IHDR chunk")
                } else {
                    val width = ((bytes[16].toInt() and 0xFF) shl 24) or
                            ((bytes[17].toInt() and 0xFF) shl 16) or
                            ((bytes[18].toInt() and 0xFF) shl 8) or
                            (bytes[19].toInt() and 0xFF)
                    val height = ((bytes[20].toInt() and 0xFF) shl 24) or
                            ((bytes[21].toInt() and 0xFF) shl 16) or
                            ((bytes[22].toInt() and 0xFF) shl 8) or
                            (bytes[23].toInt() and 0xFF)
                    if (width != 512 || height != 512) {
                        errors.add("mod icon must be 512x512, got ${width}x${height}")
                    }
                }
            }

            val noticePath = noticeFile.asFile
            if (!noticePath.exists()) {
                errors.add("cannot read ${noticePath.name}")
            } else {
                val noticeText = noticePath.readText(Charsets.UTF_8)
                val requiredNoticeValues = mapOf(
                    "Modrinth icon source" to "https://cdn.modrinth.com/data/H8peNuJG/0c9fcf0f40ec59d591b7cc17452c63a843df122e.png",
                    "immutable upstream revision" to "74ba2412a6149a1d91788c3663497c4c81992983",
                    "approved icon checksum" to "c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d",
                    "icon license" to "GPL-3.0"
                )
                for ((label, value) in requiredNoticeValues) {
                    if (!noticeText.contains(value)) {
                        errors.add("THIRD_PARTY_NOTICES.md is missing $label: $value")
                    }
                }
            }

            if (errors.isNotEmpty()) {
                throw GradleException("MOD ICON: ${errors.size} error(s)\n" + errors.joinToString("\n") { "ERROR: $it" })
            } else {
                logger.lifecycle("MOD ICON: OK (512x512, SHA-256 c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d) [fallback JVM check]")
            }
        }
    }
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
    // Do not silently use the JVM which happened to start an old Gradle daemon as javac.
    // The target Minecraft/TaCZ classes require Java 25, so select an installed JDK 25 toolchain.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}
