package me.muksc.tacztweaks.core

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.metadata.version.VersionPredicate
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull

private fun getModContainer(id: String): ModContainer? =
    FabricLoader.getInstance().getModContainer(id).getOrNull()

private fun modVersionMatch(id: String, version: String): Boolean {
    val container = getModContainer(id) ?: return false
    return VersionPredicate.parse(version).test(container.metadata.version)
}

fun getModInfo(id: String): ModInfo? {
    val container = getModContainer(id) ?: return null
    return ModInfo(
        id = container.metadata.id,
        name = container.metadata.name,
        version = container.metadata.version.friendlyString
    )
}

fun isModLoaded(id: String): Boolean =
    FabricLoader.getInstance().isModLoaded(id)

fun modVersionRange(id: String, startInclusive: String): Boolean =
    modVersionMatch(id, ">=${startInclusive}")

fun modVersionRange(id: String, startInclusive: String, endExclusive: String): Boolean =
    modVersionMatch(id, ">=${startInclusive} <${endExclusive}")

val GAME_DIR: Path = FabricLoader.getInstance().gameDir

data class ModInfo(
    val id: String,
    val name: String,
    val version: String
)
