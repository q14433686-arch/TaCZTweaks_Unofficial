import java.security.MessageDigest
import java.util.zip.ZipFile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    kotlin("jvm") version "2.4.10"
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

    // The TaCZ refabricated port we integrate with (compile only in production; tests
    // exercise its codecs and therefore need it on their runtime classpath as well).
    val taczJar = files("libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar")
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

    val metadataFile = layout.projectDirectory.file("src/main/resources/fabric.mod.json")
    val iconFile = layout.projectDirectory.file("src/main/resources/icon.png")
    val noticeFile = layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")
    inputs.files(metadataFile, iconFile, noticeFile)

    doLast {
        val expectedIconPath = "icon.png"
        val expectedSha256 = "c8591fdd552d0bbad05cd8a60136faf89d5e9fd6d0dab08eb96fa04439c6db9d"
        val rejectedPlaceholderSha256 = "5e1272a625af1b0b4d866d0fb468e1cea0a9258411f16a7d06d31a84e8953ac8"
        val upstreamCommit = "74ba2412a6149a1d91788c3663497c4c81992983"
        val modrinthSource = "https://cdn.modrinth.com/data/H8peNuJG/0c9fcf0f40ec59d591b7cc17452c63a843df122e.png"

        val metadata = metadataFile.asFile.readText(Charsets.UTF_8)
        val configuredIcon = Regex(""""icon"\s*:\s*"([^"]+)"""")
            .find(metadata)?.groupValues?.get(1)
        check(configuredIcon == expectedIconPath) {
            "fabric.mod.json icon must be '$expectedIconPath', not '$configuredIcon'"
        }

        val raw = iconFile.asFile.readBytes()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        check(digest != rejectedPlaceholderSha256) {
            "mod icon is the known dark-grey/orange placeholder"
        }
        check(digest == expectedSha256) {
            "mod icon SHA-256 drifted: expected $expectedSha256, got $digest"
        }

        val pngSignature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        )
        check(raw.size >= 24 && raw.copyOfRange(0, 8).contentEquals(pngSignature)) {
            "mod icon is not a PNG with a valid signature"
        }
        check(raw.copyOfRange(12, 16).contentEquals("IHDR".toByteArray(Charsets.US_ASCII))) {
            "mod icon does not have a leading IHDR chunk"
        }

        fun pngInt(offset: Int): Int =
            ((raw[offset].toInt() and 0xff) shl 24) or
                ((raw[offset + 1].toInt() and 0xff) shl 16) or
                ((raw[offset + 2].toInt() and 0xff) shl 8) or
                (raw[offset + 3].toInt() and 0xff)

        val width = pngInt(16)
        val height = pngInt(20)
        check(width == 512 && height == 512) {
            "mod icon must be 512x512, got ${width}x${height}"
        }

        val notice = noticeFile.asFile.readText(Charsets.UTF_8)
        mapOf(
            "Modrinth icon source" to modrinthSource,
            "immutable upstream revision" to upstreamCommit,
            "approved icon checksum" to expectedSha256,
            "icon license" to "GPL-3.0",
        ).forEach { (label, value) ->
            check(value in notice) {
                "THIRD_PARTY_NOTICES.md is missing $label: $value"
            }
        }

        logger.lifecycle("MOD ICON: OK (512x512, SHA-256 $expectedSha256)")
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
        val shaIndex = headers.indexOf("sha256")
        val bundledIndex = headers.indexOf("bundled_in_release_jar")
        check(pathIndex >= 0 && shaIndex >= 0 && bundledIndex >= 0) {
            "RESOURCE_IMPORT_MANIFEST.tsv must contain path, sha256 and bundled_in_release_jar columns"
        }
        rows.drop(1).forEach { row ->
            val columns = row.split('\t')
            val relativePath = columns.getOrNull(pathIndex).orEmpty()
            val expectedSha = columns.getOrNull(shaIndex).orEmpty()
            check(relativePath.isNotBlank() && expectedSha.matches(Regex("[0-9a-f]{64}"))) {
                "Malformed dependency manifest row: $row"
            }
            val file = layout.projectDirectory.file(relativePath).asFile
            check(file.isFile) { "Manifest dependency is missing: $relativePath" }
            val actualSha = MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes())
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            check(actualSha == expectedSha) {
                "SHA-256 mismatch for $relativePath: expected $expectedSha, got $actualSha"
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

val checkJarContents by tasks.registering {
    group = "verification"
    description = "Verifies required release jar metadata and rejects accidental bundled fixtures/local jars."

    // Minecraft 26.1+ is unobfuscated in this port, so Loom does not create a remapJar task.
    // The normal jar task is the release jar whose resources and notices must be audited.
    dependsOn("jar")

    doLast {
        val jarFile = layout.buildDirectory
            .file("libs/${project.property("archives_base_name")}-$modVersion.jar")
            .get()
            .asFile
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
        logger.lifecycle("JAR CONTENTS: OK ($jarFile)")
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
