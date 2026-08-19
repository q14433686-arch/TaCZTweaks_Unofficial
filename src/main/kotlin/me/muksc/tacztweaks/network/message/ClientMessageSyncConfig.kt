package me.muksc.tacztweaks.network.message

import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import io.netty.buffer.Unpooled
import io.netty.handler.codec.DecoderException
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
        val length = buf.readableBytes()
        require(length <= MAX_CONFIG_BYTES) { "Config payload is too large: $length" }
        out.writeInt(length)
        // Do not advance the stored buffer: one payload can be encoded more than once.
        out.writeBytes(buf, buf.readerIndex(), length)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        private const val MAX_CONFIG_BYTES = 1 shl 20
        val TYPE = CustomPacketPayload.Type<ClientMessageSyncConfig>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_sync_config")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessageSyncConfig> = StreamCodec.ofMember(
            ClientMessageSyncConfig::write,
            { source ->
                val length = source.readInt()
                if (length !in 0..MAX_CONFIG_BYTES || length > source.readableBytes()) {
                    throw DecoderException("Invalid TaCZ Tweaks config payload length: $length")
                }
                ClientMessageSyncConfig(FriendlyByteBuf(source.readBytes(length)))
            }
        )

        fun create(): ClientMessageSyncConfig =
            ClientMessageSyncConfig(FriendlyByteBuf(Unpooled.buffer()).also { Config.encode(it) })

        @Suppress("UnstableApiUsage")
        fun handle(msg: ClientMessageSyncConfig, server: MinecraftServer, player: ServerPlayer?, responseSender: PacketSender) {
            server.execute {
                if (player == null || !ConfigManager.canUpdateServerConfig(player)) {
                    msg.buf.release()
                    return@execute
                }
                val backup = FriendlyByteBuf(Unpooled.buffer()).also { Config.encode(it) }
                try {
                    Config.decode(msg.buf)
                    Config.sync(ESyncDirection.CLIENT_TO_SERVER)
                    Config.saveToFile()
                    AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
                    NetworkHandler.sendSyncConfigAll(server)
                } catch (error: RuntimeException) {
                    backup.readerIndex(0)
                    Config.decode(backup)
                    TaCZTweaks.LOGGER.warn("Rejected invalid config update from {}", player.scoreboardName, error)
                    NetworkHandler.sendSyncConfig(player)
                } finally {
                    backup.release()
                    msg.buf.release()
                }
            }
        }
    }
}
