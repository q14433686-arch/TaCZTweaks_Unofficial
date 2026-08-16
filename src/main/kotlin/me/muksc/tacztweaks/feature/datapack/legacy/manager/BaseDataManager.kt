package me.muksc.tacztweaks.feature.datapack.legacy.manager

import com.google.common.collect.ImmutableMap
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.core.logger.withMarker
import me.muksc.tacztweaks.core.resource.IdentifiableResourceReloadListener
import me.muksc.tacztweaks.core.toImmutableMap
import net.minecraft.ChatFormatting
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.resources.ResourceManager
//? if >=1.21.11 {
/*import com.tacz.guns.util.ResourceScanner
import net.minecraft.resources.FileToIdConverter
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
*///?} else {
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
//?}
import net.minecraft.util.profiling.ProfilerFiller
import org.slf4j.Logger
import org.slf4j.MarkerFactory
import kotlin.reflect.KClass

private val GSON = GsonBuilder()
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .create()

abstract class BaseDataManager<E : Any>(
    private val directory: String,
    private val elementComparator: Comparator<E>
) : /*? if >=1.21.11 {*/ /*SimplePreparableReloadListener<Map<Identifier, JsonElement>>()*/ /*?} else {*/ SimpleJsonResourceReloadListener(GSON, directory) /*?}*/, IdentifiableResourceReloadListener {
    companion object {
        val ALL by lazy { listOf(
            BulletInteractionManager,
            BulletParticlesManager,
            BulletSoundsManager,
            MeleeInteractionManager
        ) }
    }

    val logger = TaCZTweaks.logger.withMarker(
        MarkerFactory.getMarker(this::class.simpleName)
    )
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    protected var map: Map<KClass<*>, Map<ResourceLocation, E>> = emptyMap()

    protected var hasError: Boolean = false
        private set

    protected abstract val debugEnabled: Boolean

    abstract fun parseElement(json: JsonElement): E

    open fun notifyPlayer(player: ServerPlayer) {
        if (hasError) {
            player.sendSystemMessage(TaCZTweaks.message().append(
                TaCZTweaks.translatable("datapack.data_manager.error", directory)
                    .withStyle(ChatFormatting.RED)
            ))
        }
    }

    @Suppress("UNCHECKED_CAST")
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    protected inline fun <reified T: E> byType(): Map<ResourceLocation, T> =
        //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
        map.getOrElse(T::class) { emptyMap() } as Map<ResourceLocation, T>

    //? if >=1.21.11 {
    /*override fun prepare(resourceManager: ResourceManager, profiler: ProfilerFiller): Map<Identifier, JsonElement> =
        ResourceScanner.scanDirectory(resourceManager, FileToIdConverter.json(directory), GSON)
    *///?}

    override fun apply(
        //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
        elements: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        hasError = false
        //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
        map = buildMap<KClass<*>, ImmutableMap.Builder<ResourceLocation, E>> {
            for ((id, json) in elements) {
                try {
                    val element = parseElement(json)
                    computeIfAbsent(element::class) {
                        ImmutableMap.builder()
                    }.put(id, element)
                } catch (e: RuntimeException) {
                    logger.error("Parsing error loading $directory, id $id", e)
                    hasError = true
                }
            }
        }.mapValues { (_, value) ->
            value.orderEntriesByValue(elementComparator).build()
        }.toImmutableMap()
    }

    fun Logger.infoDebug(msg: String) {
        if (debugEnabled) info(msg)
    }
}