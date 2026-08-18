package me.muksc.tacztweaks.network.message

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.network.NetworkHandler
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class ClientMessageBroadcastSound(
    private val soundName: Identifier,
    private val volume: Float,
    private val pitch: Float,
    private val distance: Int
) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readIdentifier(),
        buf.readFloat(),
        buf.readFloat(),
        buf.readInt()
    )

    fun write(out: FriendlyByteBuf) {
        out.writeIdentifier(soundName)
        out.writeFloat(volume)
        out.writeFloat(pitch)
        out.writeInt(distance)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ClientMessageBroadcastSound>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_broadcast_sound")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessageBroadcastSound> = StreamCodec.ofMember(
            ClientMessageBroadcastSound::write,
            { buf -> ClientMessageBroadcastSound(buf) }
        )

        fun handle(msg: ClientMessageBroadcastSound, server: MinecraftServer, player: ServerPlayer?, responseSender: PacketSender) {
            server.execute {
                if (player == null) return@execute
                val pos = player.blockPosition()
                val distanceSqr = msg.distance * msg.distance
                server.playerList.players
                    .filter { it.distanceToSqr(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < distanceSqr }
                    .filter { it.id != player.id }
                    .forEach {
                        NetworkHandler.sendS2C(
                            it,
                            ServerMessageBroadcastSound(player.id, msg.soundName, msg.volume, msg.pitch, msg.distance)
                        )
                    }
            }
        }
    }
}
