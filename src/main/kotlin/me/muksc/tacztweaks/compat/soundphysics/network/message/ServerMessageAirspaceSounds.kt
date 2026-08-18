package me.muksc.tacztweaks.compat.soundphysics.network.message

import com.google.common.collect.Lists
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

/**
 * Airspace candidates, encoded without {@code ClientboundSoundPacket}
 * (26.2 packet write API is not the 1.20 {@code write(FriendlyByteBuf)}).
 */
class ServerMessageAirspaceSounds(
    val sounds: List<AirspaceSound>,
    val x: Double,
    val y: Double,
    val z: Double
) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readCollection(Lists::newArrayListWithCapacity, AirspaceSound::read),
        buf.readDouble(),
        buf.readDouble(),
        buf.readDouble()
    )

    fun write(out: FriendlyByteBuf) {
        out.writeCollection(sounds) { buf, element -> element.write(buf) }
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
        fun write(out: FriendlyByteBuf) {
            out.writeCollection(sounds) { buf, spec -> spec.write(buf) }
            out.writeFloat(minAirspace)
            out.writeFloat(maxAirspace)
            out.writeFloat(minOcclusion)
            out.writeFloat(maxOcclusion)
            out.writeFloat(minReflectivity)
            out.writeFloat(maxReflectivity)
        }

        fun canPlayAtAirspace(airspace: Float): Boolean = airspace in minAirspace..maxAirspace
        fun canPlayAtOcclusion(occlusion: Double): Boolean = occlusion in minOcclusion.toDouble()..maxOcclusion.toDouble()
        fun canPlayAtReflectivity(reflectivity: Float): Boolean = reflectivity in minReflectivity..maxReflectivity

        companion object {
            fun read(buf: FriendlyByteBuf): AirspaceSound = AirspaceSound(
                buf.readCollection(Lists::newArrayListWithCapacity, SoundSpec::read),
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
