package me.muksc.tacztweaks.network.message

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.mixininterface.gun.SlideDataHolder
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class ClientMessagePlayerShouldSlide private constructor(private val shouldSlide: Boolean) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(buf.readBoolean())

    fun write(out: FriendlyByteBuf) {
        out.writeBoolean(shouldSlide)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ClientMessagePlayerShouldSlide>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_player_should_slide")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessagePlayerShouldSlide> = StreamCodec.ofMember(
            ClientMessagePlayerShouldSlide::write,
            { buf -> ClientMessagePlayerShouldSlide(buf) }
        )

        @JvmStatic
        fun create(shouldSlide: Boolean): ClientMessagePlayerShouldSlide = ClientMessagePlayerShouldSlide(shouldSlide)

        fun handle(msg: ClientMessagePlayerShouldSlide, server: MinecraftServer, player: ServerPlayer?, responseSender: PacketSender) {
            server.execute {
                (player as? SlideDataHolder)?.`tacztweaks$setShouldSlide`(msg.shouldSlide)
            }
        }
    }
}
