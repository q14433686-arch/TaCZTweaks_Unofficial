package me.muksc.tacztweaks.compat.soundphysics.network.message

import io.netty.handler.codec.DecoderException
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level

private const val MAX_CANDIDATES = 64
private const val MAX_SOUNDS_PER_CANDIDATE = 32

private fun <T> readBoundedList(
    buf: FriendlyByteBuf,
    maxSize: Int,
    decoder: (FriendlyByteBuf) -> T
): List<T> {
    val size = buf.readVarInt()
    if (size !in 0..maxSize) throw DecoderException("Collection size $size exceeds limit $maxSize")
    return List(size) { decoder(buf) }
}

private fun <T> writeBoundedList(
    buf: FriendlyByteBuf,
    values: List<T>,
    maxSize: Int,
    encoder: (FriendlyByteBuf, T) -> Unit
) {
    val bounded = values.take(maxSize)
    buf.writeVarInt(bounded.size)
    bounded.forEach { encoder(buf, it) }
}

/** Bounded airspace candidates encoded independently of ClientboundSoundPacket. */
class ServerMessageAirspaceSounds(
    val sounds: List<AirspaceSound>,
    val x: Double,
    val y: Double,
    val z: Double
) : CustomPacketPayload {
    init {
        require(sounds.size <= MAX_CANDIDATES) { "Too many airspace candidates" }
        require(x.isFinite() && y.isFinite() && z.isFinite()) { "Airspace position must be finite" }
    }

    constructor(buf: FriendlyByteBuf) : this(
        readBoundedList(buf, MAX_CANDIDATES, AirspaceSound::read),
        buf.readDouble(),
        buf.readDouble(),
        buf.readDouble()
    )

    fun write(out: FriendlyByteBuf) {
        writeBoundedList(out, sounds, MAX_CANDIDATES) { buf, element -> element.write(buf) }
        out.writeDouble(x)
        out.writeDouble(y)
        out.writeDouble(z)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerMessageAirspaceSounds>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "airspace_sounds")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ServerMessageAirspaceSounds> = StreamCodec.ofMember(
            ServerMessageAirspaceSounds::write,
            { buf -> ServerMessageAirspaceSounds(buf) }
        )

        fun handle(msg: ServerMessageAirspaceSounds, client: Minecraft) {
            if (!SoundPhysicsCompat.isEnabled()) return
            client.execute { SoundPhysicsCompat.play(client, msg) }
        }
    }

    class AirspaceSound(
        val sounds: List<SoundSpec>,
        val minAirspace: Float,
        val maxAirspace: Float,
        val minOcclusion: Float,
        val maxOcclusion: Float,
        val minReflectivity: Float,
        val maxReflectivity: Float
    ) {
        init {
            require(sounds.size <= MAX_SOUNDS_PER_CANDIDATE) { "Too many airspace sounds" }
            require(validRange(minAirspace, maxAirspace)) { "Invalid airspace range" }
            require(validRange(minOcclusion, maxOcclusion)) { "Invalid occlusion range" }
            require(validRange(minReflectivity, maxReflectivity)) { "Invalid reflectivity range" }
        }

        fun write(out: FriendlyByteBuf) {
            writeBoundedList(out, sounds, MAX_SOUNDS_PER_CANDIDATE) { buf, spec -> spec.write(buf) }
            out.writeFloat(minAirspace)
            out.writeFloat(maxAirspace)
            out.writeFloat(minOcclusion)
            out.writeFloat(maxOcclusion)
            out.writeFloat(minReflectivity)
            out.writeFloat(maxReflectivity)
        }

        fun canPlayAtAirspace(airspace: Float): Boolean = airspace.isFinite() && airspace in minAirspace..maxAirspace
        fun canPlayAtOcclusion(occlusion: Double): Boolean = occlusion.isFinite() && occlusion in minOcclusion.toDouble()..maxOcclusion.toDouble()
        fun canPlayAtReflectivity(reflectivity: Float): Boolean = reflectivity.isFinite() && reflectivity in minReflectivity..maxReflectivity

        companion object {
            private fun validRange(min: Float, max: Float): Boolean =
                min.isFinite() && max.isFinite() && min <= max

            fun read(buf: FriendlyByteBuf): AirspaceSound = AirspaceSound(
                readBoundedList(buf, MAX_SOUNDS_PER_CANDIDATE, SoundSpec::read),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
            )
        }
    }

    class SoundSpec(
        val sound: Identifier,
        val volume: Float,
        val pitch: Float,
        val range: Float?
    ) {
        init {
            require(volume.isFinite() && volume in 0.0F..4.0F) { "Invalid sound volume" }
            require(pitch.isFinite() && pitch in 0.01F..4.0F) { "Invalid sound pitch" }
            require(range == null || range.isFinite() && range in 0.01F..256.0F) { "Invalid sound range" }
        }

        fun write(out: FriendlyByteBuf) {
            out.writeIdentifier(sound)
            out.writeFloat(volume)
            out.writeFloat(pitch)
            out.writeBoolean(range != null)
            if (range != null) out.writeFloat(range)
        }

        fun play(level: Level, x: Double, y: Double, z: Double) {
            val event = if (range == null) SoundEvent.createVariableRangeEvent(sound)
            else SoundEvent.createFixedRangeEvent(sound, range)
            level.playLocalSound(x, y, z, event, SoundSource.PLAYERS, volume, pitch, false)
        }

        companion object {
            fun read(buf: FriendlyByteBuf): SoundSpec {
                val sound = buf.readIdentifier()
                val volume = buf.readFloat()
                val pitch = buf.readFloat()
                val range = if (buf.readBoolean()) buf.readFloat() else null
                return SoundSpec(sound, volume, pitch, range)
            }
        }
    }
}
