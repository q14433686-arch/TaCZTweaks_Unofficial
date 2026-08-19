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

val checkVendoredDependencies by tasks.registering {
    group = "verification"
    description = "Verifies vendored binary dependencies against RESOURCE_IMPORT_MANIFEST.tsv."

    val manifest = layout.projectDirectory.file("RESOURCE_IMPORT_MANIFEST.tsv")
    inputs.file(manifest)
    inputs.dir(layout.projectDirectory.dir("libs"))

    doLast {
        val rows = manifest.asFile.readLines(Charsets.UTF_8)
            .filter { it.isNotBlank() && !it.startsWith("#") }
        check(rows.isNotEmpty()) { "RESOURCE_IMPORT_MANIFEST.tsv is empty" }
        val headers = rows.first().split('\t')
        val pathIndex = headers.indexOf("path")
        val sha256Index = headers.indexOf("sha256")
        val sha512Index = headers.indexOf("sha512")
        val bundledIndex = headers.indexOf("bundled_in_release_jar")
        check(pathIndex >= 0 && bundledIndex >= 0 && (sha256Index >= 0 || sha512Index >= 0)) {
            "RESOURCE_IMPORT_MANIFEST.tsv must contain path, bundled_in_release_jar and sha256/sha512 columns"
        }
        rows.drop(1).forEach { row ->
            val columns = row.split('\t')
            val relativePath = columns.getOrNull(pathIndex).orEmpty()
            val expectedSha256 = columns.getOrNull(sha256Index).orEmpty()
            val expectedSha512 = columns.getOrNull(sha512Index).orEmpty()
            check(relativePath.isNotBlank()) { "Malformed dependency manifest row: $row" }
            check(expectedSha256.matches(Regex("[0-9a-f]{64}")) || expectedSha512.matches(Regex("[0-9a-f]{128}"))) {
                "Manifest row for $relativePath must declare sha256 and/or sha512"
            }
            val file = layout.projectDirectory.file(relativePath).asFile
            check(file.isFile) { "Manifest dependency is missing: $relativePath" }
            if (expectedSha256.matches(Regex("[0-9a-f]{64}"))) {
                val actualSha = MessageDigest.getInstance("SHA-256")
                    .digest(file.readBytes())
                    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                check(actualSha == expectedSha256) {
                    "SHA-256 mismatch for $relativePath: expected $expectedSha256, got $actualSha"
                }
            }
            if (expectedSha512.matches(Regex("[0-9a-f]{128}"))) {
                val actualSha = MessageDigest.getInstance("SHA-512")
                    .digest(file.readBytes())
                    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                check(actualSha == expectedSha512) {
                    "SHA-512 mismatch for $relativePath: expected $expectedSha512, got $actualSha"
                }
            }
        }
        logger.lifecycle("VENDORED DEPENDENCIES: OK (${rows.size - 1} files)")
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

// 1.21.11 is obfuscated and this project uses fabric-loom-remap, so the published
// artifact is remapJar. Do not copy the unobfuscated 26.2 dependsOn("jar") path,
// and do not assume remapJar exists on every future Loom setup.
val releaseJarTaskName = if (tasks.names.contains("remapJar")) "remapJar" else "jar"

val checkJarContents by tasks.registering {
    group = "verification"
    description = "Verifies required release jar metadata and rejects accidental bundled fixtures/local jars."
    dependsOn(releaseJarTaskName)

    doLast {
        val archiveTask = tasks.named(releaseJarTaskName).get()
        val jarFile = if (archiveTask is org.gradle.api.tasks.bundling.AbstractArchiveTask) {
            archiveTask.archiveFile.get().asFile
        } else {
            layout.buildDirectory
                .file("libs/${project.property("archives_base_name")}-$modVersion.jar")
                .get()
                .asFile
        }
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
                    name.startsWith("libs/") ||
                    name == "yacl-fabric.jar" ||
                    name.startsWith("TACZ-Refabricated-")
            }.toList()
            check(forbidden.isEmpty()) { "Release jar contains forbidden entries: $forbidden" }
        }
        logger.lifecycle("JAR CONTENTS: OK ($jarFile via $releaseJarTaskName)")
    }
}

tasks.named("check") {
    dependsOn(checkModIcon, checkVendoredDependencies, checkJarContents)
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
