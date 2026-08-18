package me.muksc.tacztweaks.network.message

import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import io.netty.buffer.Unpooled
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.config.sync.ESyncDirection
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class ServerMessageSyncConfig private constructor(private val buf: FriendlyByteBuf) : CustomPacketPayload {
    fun write(out: FriendlyByteBuf) {
        out.writeInt(buf.writerIndex())
        out.writeBytes(buf)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerMessageSyncConfig>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "server_sync_config")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ServerMessageSyncConfig> = StreamCodec.ofMember(
            ServerMessageSyncConfig::write,
            { buf -> ServerMessageSyncConfig(FriendlyByteBuf(buf.readBytes(buf.readInt()))) }
        )

        fun create(): ServerMessageSyncConfig =
            ServerMessageSyncConfig(FriendlyByteBuf(Unpooled.buffer()).also { Config.encode(it) })

        fun handle(msg: ServerMessageSyncConfig, client: Minecraft) {
            Config.decode(msg.buf)
            Config.sync(ESyncDirection.SERVER_TO_CLIENT)
            client.player?.also { player ->
                AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
            }
        }
    }
}
