package me.muksc.tacztweaks.compat.soundphysics

import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageAirspaceSounds
import me.muksc.tacztweaks.data.manager.BulletSoundsManager
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import java.util.UUID

object SoundPhysicsCompat {
    private val enabled: Boolean = FabricLoader.getInstance().isModLoaded("sound_physics_remastered")
    private val pendingSounds: MutableMap<UUID, ServerMessageAirspaceSounds> = mutableMapOf()
    var processingSound: ProcessingSound? = null

    fun isEnabled(): Boolean = enabled

    fun play(minecraft: Minecraft, packet: ServerMessageAirspaceSounds) {
        if (!enabled) return
        val uuid = UUID.randomUUID()
        pendingSounds[uuid] = packet
        minecraft.soundManager.play(SoundPhysicsTriggerSoundInstance(uuid, packet.x, packet.y, packet.z))
    }

    fun onSoundEvaluationComplete() {
        val processing = processingSound ?: return
        val pending = pendingSounds[processing.sound.uuid] ?: return

        val airspace = processing.airspace ?: return
        val occlusionAccumulation = processing.occlusionAccumulation ?: return
        val reflectivity = processing.reflectivity?.div(processing.reflectivityDivider.coerceAtLeast(1)) ?: return
        BulletSoundsManager.logAirspace { "airspace: $airspace, occlusion: $occlusionAccumulation, reflectivity: $reflectivity" }
        val sound = pending.sounds.firstOrNull {
            it.canPlayAtAirspace(airspace)
                && it.canPlayAtOcclusion(occlusionAccumulation)
                && it.canPlayAtReflectivity(reflectivity)
        } ?: return

        val minecraft = Minecraft.getInstance()
        minecraft.execute {
            val level = minecraft.level ?: return@execute
            for (spec in sound.sounds) {
                spec.play(level, pending.x, pending.y, pending.z)
            }
        }
    }

    fun runProcessing(sound: SoundPhysicsTriggerSoundInstance, block: Runnable) {
        try {
            processingSound = ProcessingSound(sound)
            block.run()
        } finally {
            onSoundEvaluationComplete()
            processingSound = null
            pendingSounds.remove(sound.uuid)
        }
    }

    class ProcessingSound(val sound: SoundPhysicsTriggerSoundInstance) {
        var airspace: Float? = null
        var occlusionAccumulation: Double? = null
        var reflectivity: Float? = null
        var reflectivityDivider: Int = 1
    }
}
