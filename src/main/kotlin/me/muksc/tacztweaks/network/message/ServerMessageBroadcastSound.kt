package me.muksc.tacztweaks.network.message

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.network.ClientPacketBridge
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

class ServerMessageBroadcastSound(
    val entityId: Int,
    val soundName: Identifier,
    val volume: Float,
    val pitch: Float,
    val distance: Int
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
            ctx.enqueueWork {
                ClientPacketBridge.invoke(
                    "onBroadcastSound",
                    arrayOf<Class<*>>(ServerMessageBroadcastSound::class.java),
                    msg
                )
            }
        }
    }
}
