@file:Suppress("unused")
package me.muksc.tacztweaks.core.compatibility

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.core.logger.withMarker
import me.muksc.tacztweaks.feature.sound_physics_evaluate.SoundPhysicsManager
import org.slf4j.MarkerFactory

abstract class ModCompatibilityManager(
    val modId: String,
    val mixinPackagePrefix: String?
) {
    companion object {
        // 26.2: most of the 1.20.1/1.21.1 optional-compat mods (Valkyrien Skies, Sable,
        // FirstAid, LSO, PillagersGun, Cuffed, MTS, LRTactical) have no 26.2 build available.
        // Only Sound Physics Remastered (which has a 26.2 fabric build) is retained.
        // See docs/PORT_PLAN_26.2.md §6.4 for the per-mod availability matrix.
        val ALL by lazy { listOf(
            SoundPhysicsManager
        ) }
    }

    @JvmField val logger = TaCZTweaks.logger.withMarker(
        MarkerFactory.getMarker("ModCompatibilityManager")
    )
    @JvmField val loaded = isModLoaded(modId)
    var hasError = false
        protected set

    fun onError(message: String, throwable: Throwable? = null) {
        hasError = true
        logger.error(message, throwable)
    }

    fun <T> withFallback(fallback: T, block: () -> T): T {
        if (!loaded) return fallback
        return try {
            block()
        } catch (e: Exception) {
            onError("Encountered an error while executing a '${modId}' mod compatibility code", e)
            fallback
        }
    }

    open fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean = true
}
