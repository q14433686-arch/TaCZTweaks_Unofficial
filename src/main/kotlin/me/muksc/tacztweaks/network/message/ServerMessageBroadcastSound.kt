package me.muksc.tacztweaks.network.message
import net.neoforged.neoforge.network.handling.IPayloadContext

import com.tacz.guns.client.sound.SoundPlayManager
import me.muksc.tacztweaks.TaCZTweaks
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class ServerMessageBroadcastSound(
    private val entityId: Int,
    private val soundName: Identifier,
    private val volume: Float,
    private val pitch: Float,
    private val distance: Int
) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readInt(),
        buf.readIdentifier(),
        buf.readFloat(),
        buf.readFloat(),
        buf.readInt()
    )

    fun write(out: FriendlyByteBuf) {
        out.writeInt(entityId)
        out.writeIdentifier(soundName)
        out.writeFloat(volume)
        out.writeFloat(pitch)
        out.writeInt(distance)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerMessageBroadcastSound>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "server_broadcast_sound")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ServerMessageBroadcastSound> = StreamCodec.ofMember(
            ServerMessageBroadcastSound::write,
            { buf -> ServerMessageBroadcastSound(buf) }
        )

        fun handle(msg: ServerMessageBroadcastSound, ctx: IPayloadContext) {
            val client = Minecraft.getInstance()
            val entity = client.level?.getEntity(msg.entityId) ?: return
            ctx.enqueueWork {
                SoundPlayManager.playClientSound(
                    entity,
                    msg.soundName,
                    msg.volume,
                    msg.pitch,
                    msg.distance,
                    true
                )
            }
        }
    }
}
