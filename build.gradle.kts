import groovy.json.JsonSlurper
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32
import java.util.zip.ZipFile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom-remap")
    kotlin("jvm") version "2.4.10"
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
    // TaCZ jar is downloaded from GitHub Releases to libs/ by the downloadTaczJar task.
    // YACL is resolved from Modrinth Maven.
    flatDir { dirs("libs") }
    maven { url = uri("https://api.modrinth.com/maven") }
}

// TaCZ download URL and local file name
val taczDownloadUrl = "https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases/download/1.21.11_R2/TACZ-Refabricated-1.21.11-1.1.8%2Bfabric.1.21.11.R2.jar"
val taczLocalName = "TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar"
val taczLocalFile = layout.projectDirectory.file("libs/$taczLocalName")

// Downloads the TaCZ jar from GitHub Release to libs/ before compilation.
// Upgrade: change taczDownloadUrl and taczLocalName for the new GitHub release.
val downloadTaczJar by tasks.registering {
    description = "Downloads TaCZ Refabricated jar from GitHub Releases to libs/"
    outputs.file(taczLocalFile)
    doLast {
        taczLocalFile.asFile.parentFile.mkdirs()
        logger.lifecycle("Downloading TaCZ jar from $taczDownloadUrl...")
        java.net.URL(taczDownloadUrl).openStream().use { input ->
            taczLocalFile.asFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        logger.lifecycle("Downloaded $taczLocalName")
    }
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

    // TaCZ — downloaded from GitHub Releases to libs/ by downloadTaczJar
    modCompileOnly(files(taczLocalFile).builtBy(downloadTaczJar))
    testRuntimeOnly(files(taczLocalFile).builtBy(downloadTaczJar))

    // YACL from Modrinth Maven (3.8.2+1.21.11-fabric)
    modImplementation("maven.modrinth:yacl:3.8.2+1.21.11-fabric")

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
        fun supportsPython3(command: List<String>): Boolean = try {
            val probe = ProcessBuilder(
                command + listOf(
                    "-c",
                    "import sys; raise SystemExit(0 if sys.version_info >= (3, 8) else 1)",
                )
            )
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            val finished = probe.waitFor(5, TimeUnit.SECONDS)
            if (!finished) probe.destroyForcibly()
            finished && probe.exitValue() == 0
        } catch (_: Exception) {
            false
        }

        val configuredPython = providers.gradleProperty("tacztweaks.python").orNull
            ?: System.getenv("PYTHON")
        val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val candidates = buildList {
            configuredPython?.takeIf { it.isNotBlank() }?.let { add(listOf(it)) }
            if (isWindows) add(listOf("py", "-3"))
            add(listOf("python3"))
            add(listOf("python"))
        }.distinct()
        val python = candidates.firstOrNull(::supportsPython3)
        if (python != null) {
            logger.lifecycle("checkModIcon: using ${python.joinToString(" ")}")
            val process = ProcessBuilder(python + checker.asFile.absolutePath)
                .inheritIO()
                .start()
            if (process.waitFor() != 0) {
                throw GradleException("scripts/check_mod_icon.py rejected the distributed mod icon")
            }
            return@doLast
        }

        logger.lifecycle("checkModIcon: Python 3 unavailable; using the equivalent JVM validator")
        val errors = mutableListOf<String>()
        val expectedSha256 = "c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d"
        val placeholderSha256 = "5e1272a625af1b0b4d866d0fb468e1cea0a9258411f16a7d06d31a84e8953ac8"

        try {
            val metadata = JsonSlurper().parseText(metadataFile.asFile.readText(Charsets.UTF_8))
            val configuredIcon = (metadata as? Map<*, *>)?.get("icon")
            if (configuredIcon != "icon.png") {
                errors += "fabric.mod.json icon must be 'icon.png', not $configuredIcon"
            }
        } catch (exception: Exception) {
            errors += "cannot parse fabric.mod.json: ${exception.message}"
        }

        if (!iconFile.asFile.isFile) {
            errors += "cannot read src/main/resources/icon.png: file does not exist"
        } else {
            val raw = iconFile.asFile.readBytes()
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(raw)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            when (digest) {
                placeholderSha256 -> errors += "mod icon is the known dark-grey/orange placeholder"
                expectedSha256 -> Unit
                else -> errors += "mod icon SHA-256 drifted: expected $expectedSha256, got $digest"
            }

            val signature = byteArrayOf(
                0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
            )
            val ihdrType = byteArrayOf(0x49, 0x48, 0x44, 0x52)
            fun readUnsignedInt(offset: Int): Long =
                ((raw[offset].toLong() and 0xff) shl 24) or
                    ((raw[offset + 1].toLong() and 0xff) shl 16) or
                    ((raw[offset + 2].toLong() and 0xff) shl 8) or
                    (raw[offset + 3].toLong() and 0xff)

            if (
                raw.size < 33 ||
                !raw.copyOfRange(0, 8).contentEquals(signature) ||
                readUnsignedInt(8) != 13L ||
                !raw.copyOfRange(12, 16).contentEquals(ihdrType)
            ) {
                errors += "mod icon is not a PNG with a valid leading IHDR chunk"
            } else {
                val crc = CRC32().apply { update(raw, 12, 17) }.value
                if (readUnsignedInt(29) != crc) {
                    errors += "mod icon has an invalid leading IHDR CRC"
                }
                val width = readUnsignedInt(16)
                val height = readUnsignedInt(20)
                if (width != 512L || height != 512L) {
                    errors += "mod icon must be 512x512, got ${width}x${height}"
                }
            }
        }

        try {
            val notice = noticeFile.asFile.readText(Charsets.UTF_8)
            val requiredNoticeValues = listOf(
                "https://github.com/MUKSC/TaCZTweaks",
                "74ba2412a6149a1d91788c3663497c4c81992983",
                "https://github.com/MUKSC/TaCZTweaks/blob/74ba2412a6149a1d91788c3663497c4c81992983/src/main/resources/icon.png",
                "https://cdn.modrinth.com/data/H8peNuJG/0c9fcf0f40ec59d591b7cc17452c63a843df122e.png",
                "MUKSC",
                "GPL-3.0",
                expectedSha256,
                "src/main/resources/icon.png",
            )
            requiredNoticeValues.filterNot { notice.contains(it) }.forEach {
                errors += "THIRD_PARTY_NOTICES.md is missing required icon provenance: $it"
            }
        } catch (exception: Exception) {
            errors += "cannot read THIRD_PARTY_NOTICES.md: ${exception.message}"
        }

        if (errors.isNotEmpty()) {
            throw GradleException("MOD ICON: ${errors.size} error(s)\n" + errors.joinToString("\n") { "ERROR: $it" })
        }
        logger.lifecycle("MOD ICON: OK (512x512, SHA-256 $expectedSha256; JVM validator)")
    }
}

tasks.named<Jar>("jar") {
    from(layout.projectDirectory.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE_tacztweaks" }
    }
    from(layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
        rename { "THIRD_PARTY_NOTICES_tacztweaks.md" }
    }
}

val checkJarContents by tasks.registering {
    group = "verification"
    description = "Verifies required release jar metadata and rejects accidental bundled fixtures/local jars."

    // This branch runs Loom in remap mode (fabric-loom-remap + official Mojang mappings,
    // Minecraft 1.21.11 is obfuscated), so the publishable artifact is the remapped jar
    // produced by the remapJar task. Fall back to the plain jar only for unobfuscated
    // environments (or plugin setups) that do not create a remapJar task.
    val remapJarTask = tasks.findByName("remapJar") as? org.gradle.jvm.tasks.Jar
    val releaseJarTask: org.gradle.jvm.tasks.Jar =
        remapJarTask ?: tasks.named<org.gradle.jvm.tasks.Jar>("jar").get()
    dependsOn(releaseJarTask)

    doLast {
        val jarFile = releaseJarTask.archiveFile.get().asFile
        check(jarFile.isFile) { "Expected release jar does not exist: $jarFile" }
        ZipFile(jarFile).use { zip ->
            fun requireEntry(name: String) {
                check(zip.getEntry(name) != null) { "Release jar is missing $name" }
            }
            requireEntry("fabric.mod.json")
            requireEntry("icon.png")
            requireEntry("tacztweaks.mixins.json")
            requireEntry("META-INF/LICENSE_tacztweaks")
            requireEntry("META-INF/THIRD_PARTY_NOTICES_tacztweaks.md")

            val metadata = zip.getInputStream(zip.getEntry("fabric.mod.json")).reader(Charsets.UTF_8).readText()
            check("\${version}" !in metadata) { "fabric.mod.json version placeholder was not expanded" }
            check("\"$modVersion\"" in metadata) {
                "fabric.mod.json does not contain expanded version $modVersion"
            }
            check("\"contact\"" in metadata && "TaCZTweaks_Unofficial/issues" in metadata) {
                "fabric.mod.json contact metadata is incomplete"
            }

            val forbidden = zip.entries().asSequence().map { it.name }.filter { name ->
                name.startsWith("fixtures/") ||
                    name.startsWith("src/test/") ||
                    name.endsWith(".log") ||
                    name.startsWith("libs/")
            }.toList()
            check(forbidden.isEmpty()) { "Release jar contains forbidden entries: $forbidden" }
        }
        logger.lifecycle("JAR CONTENTS: OK (${releaseJarTask.name} -> $jarFile)")
    }
}

tasks.named("check") {
    dependsOn(checkModIcon, checkJarContents)
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