package me.muksc.tacztweaks.compat.soundphysics

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageAirspaceSounds
import me.muksc.tacztweaks.data.manager.BulletSoundsManager
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/** Client-side bridge to Sound Physics Remastered 1.5.1+26.2. */
object SoundPhysicsCompat {
    private val enabled = FabricLoader.getInstance().isModLoaded("sound_physics_remastered")
    private val triggerId = Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "sound_physics_trigger")
    private val pending = ConcurrentHashMap<PositionKey, ConcurrentLinkedDeque<PendingSound>>()
    private val processing = ThreadLocal<ProcessingSound?>()

    fun isEnabled(): Boolean = enabled

    fun play(minecraft: Minecraft, packet: ServerMessageAirspaceSounds) {
        if (!enabled) return
        discardStale()
        val queue = pending.computeIfAbsent(PositionKey(packet.x, packet.y, packet.z)) { ConcurrentLinkedDeque() }
        if (queue.size >= 64) return
        queue.addLast(PendingSound(packet, System.currentTimeMillis()))
        minecraft.soundManager.play(
            SoundPhysicsTriggerSoundInstance(UUID.randomUUID(), packet.x, packet.y, packet.z)
        )
    }

    /** Called at SPR evaluateEnvironment HEAD; returns true only for our probe sound. */
    fun begin(x: Double, y: Double, z: Double, sound: Identifier): Boolean {
        if (!enabled || sound != triggerId) return false
        val key = PositionKey(x, y, z)
        val queue = pending[key] ?: return false
        val packet = queue.pollFirst()?.packet ?: return false
        if (queue.isEmpty()) pending.remove(key, queue)
        processing.set(ProcessingSound(packet))
        return true
    }

    fun captureOcclusion(value: Double) {
        processing.get()?.occlusion = value
    }

    fun captureAirspaceCount(sharedAirspaces: Int) {
        val divisor = rayDivisor()
        processing.get()?.airspace = sharedAirspaces * 64.0F / divisor
    }

    fun complete(bounceReflectivity: FloatArray?) {
        val current = processing.get() ?: return
        if (bounceReflectivity != null && bounceReflectivity.isNotEmpty()) {
            current.reflectivity = bounceReflectivity.average().toFloat()
        }
        val airspace = current.airspace ?: return
        val occlusion = current.occlusion ?: return
        val reflectivity = current.reflectivity ?: return
        val packet = current.packet
        BulletSoundsManager.logAirspace {
            "airspace=$airspace, occlusion=$occlusion, reflectivity=$reflectivity"
        }
        val selected = packet.sounds.firstOrNull {
            it.canPlayAtAirspace(airspace) &&
                it.canPlayAtOcclusion(occlusion) &&
                it.canPlayAtReflectivity(reflectivity)
        } ?: return

        Minecraft.getInstance().execute {
            val level = Minecraft.getInstance().level ?: return@execute
            selected.sounds.forEach { it.play(level, packet.x, packet.y, packet.z) }
        }
    }

    fun clear() {
        processing.remove()
    }

    /**
     * SPR defines shared airspace as count * 64 / (ray count * bounce count). Read the
     * user's live config reflectively so this optional integration has no hard dependency.
     */
    private fun rayDivisor(): Float = try {
        val mod = Class.forName("com.sonicether.soundphysics.SoundPhysicsMod")
        val config = mod.getField("CONFIG").get(null)
        val countEntry = config.javaClass.getField("environmentEvaluationRayCount").get(config)
        val bounceEntry = config.javaClass.getField("environmentEvaluationRayBounces").get(config)
        val count = (countEntry.javaClass.getMethod("get").invoke(countEntry) as Number).toInt()
        val bounces = (bounceEntry.javaClass.getMethod("get").invoke(bounceEntry) as Number).toInt()
        (count * bounces).coerceAtLeast(1).toFloat()
    } catch (error: ReflectiveOperationException) {
        TaCZTweaks.LOGGER.warn("Could not read Sound Physics ray settings; using 1.5.1 defaults", error)
        128.0F // 32 rays * 4 bounces
    }

    private fun discardStale() {
        val cutoff = System.currentTimeMillis() - 10_000L
        pending.entries.removeIf { (_, queue) ->
            while (queue.peekFirst()?.createdAt?.let { it < cutoff } == true) queue.pollFirst()
            queue.isEmpty()
        }
    }

    private data class PositionKey(val x: Double, val y: Double, val z: Double)
    private data class PendingSound(val packet: ServerMessageAirspaceSounds, val createdAt: Long)

    private class ProcessingSound(val packet: ServerMessageAirspaceSounds) {
        var airspace: Float? = null
        var occlusion: Double? = null
        var reflectivity: Float? = null
    }
}
