package me.muksc.tacztweaks.data.manager

import com.mojang.logging.LogUtils
import com.mojang.serialization.Codec
import me.muksc.tacztweaks.TaCZTweaks
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import org.slf4j.Logger
import kotlin.reflect.KClass

/**
 * Loads data-driven JSON files from the `data/<namespace>/<directory>` folder
 * (e.g. `data/tacztweaks/bullet_interactions/glass.json`), parses them with the element
 * codec, groups the results by concrete class and sorts each group by priority.
 *
 * 26.1.2 note: the vanilla [SimpleJsonResourceReloadListener] now takes a [Codec] and a
 * [FileToIdConverter] (the Gson-based ctor is gone), so `parseElement` and the old-format
 * fallback of the original Forge version are dropped.
 *
 * NeoForge note: registration goes through `AddServerReloadListenersEvent#addListener(Identifier,
 * PreparableReloadListener)` in [me.muksc.tacztweaks.TaCZTweaks]; this class only supplies the id.
 */
abstract class BaseDataManager<E : Any>(
    private val directory: String,
    private val codec: Codec<E>,
    private val elementComparator: Comparator<E>
) : SimpleJsonResourceReloadListener<E>(codec, FileToIdConverter.json(directory)) {
    protected val logger: Logger = LogUtils.getLogger()
    protected var map: Map<KClass<*>, Map<Identifier, E>> = emptyMap()

    abstract fun debugEnabled(): Boolean

    fun logDebug(msg: () -> String) {
        if (debugEnabled()) logger.info(msg.invoke())
    }

    /** Reload-listener id used by `AddServerReloadListenersEvent` (was the Fabric loader id). */
    fun id(): Identifier =
        Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "data/" + directory)

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified T : E> byType(): Map<Identifier, T> =
        map.getOrElse(T::class) { emptyMap() } as Map<Identifier, T>

    @Suppress("UNCHECKED_CAST")
    override fun apply(elements: Map<Identifier, E>, resourceManager: ResourceManager, profiler: ProfilerFiller) {
        val grouped = elements.entries.groupBy({ it.value::class }, { it.toPair() })
        val byValue = Comparator<Pair<Identifier, E>> { a, b -> elementComparator.compare(a.second, b.second) }
        map = grouped.mapValues { (_, entries) ->
            entries.sortedWith(byValue).associate { it }
        } as Map<KClass<*>, Map<Identifier, E>>
        logDebug { "Reloaded '$directory': ${elements.size} file(s)" }
    }
}
