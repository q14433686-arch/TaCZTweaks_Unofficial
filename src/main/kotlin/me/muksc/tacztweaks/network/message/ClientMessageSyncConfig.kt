package me.muksc.tacztweaks.network.message

import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import io.netty.buffer.Unpooled
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.config.ConfigManager
import me.muksc.tacztweaks.config.sync.ESyncDirection
import me.muksc.tacztweaks.network.NetworkHandler
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class ClientMessageSyncConfig private constructor(private val buf: FriendlyByteBuf) : CustomPacketPayload {
    fun write(out: FriendlyByteBuf) {
        out.writeInt(buf.writerIndex())
        out.writeBytes(buf)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ClientMessageSyncConfig>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_sync_config")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessageSyncConfig> = StreamCodec.ofMember(
            ClientMessageSyncConfig::write,
            { buf -> ClientMessageSyncConfig(FriendlyByteBuf(buf.readBytes(buf.readInt()))) }
        )

        fun create(): ClientMessageSyncConfig =
            ClientMessageSyncConfig(FriendlyByteBuf(Unpooled.buffer()).also { Config.encode(it) })

        @Suppress("UnstableApiUsage")
        fun handle(msg: ClientMessageSyncConfig, server: MinecraftServer, player: ServerPlayer?, responseSender: PacketSender) {
            if (player == null) return
            if (!ConfigManager.canUpdateServerConfig(player)) return
            Config.decode(msg.buf)
            Config.sync(ESyncDirection.CLIENT_TO_SERVER)
            Config.saveToFile()

            AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
            NetworkHandler.sendSyncConfigAll(server)
        }
    }
}
