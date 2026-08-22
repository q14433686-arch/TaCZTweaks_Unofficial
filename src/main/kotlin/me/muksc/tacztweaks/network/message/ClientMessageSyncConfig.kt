package me.muksc.tacztweaks.network.message
import net.neoforged.neoforge.network.handling.IPayloadContext

import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import io.netty.buffer.Unpooled
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.config.ConfigManager
import me.muksc.tacztweaks.config.sync.ESyncDirection
import me.muksc.tacztweaks.network.NetworkHandler
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class ClientMessageSyncConfig private constructor(private val payload: ByteArray) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(readPayload(buf))

    fun write(out: FriendlyByteBuf) {
        out.writeInt(payload.size)
        out.writeBytes(payload)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        private const val MAX_BYTES = 1 shl 20

        val TYPE = CustomPacketPayload.Type<ClientMessageSyncConfig>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_sync_config")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessageSyncConfig> = StreamCodec.ofMember(
            ClientMessageSyncConfig::write,
            { buf -> ClientMessageSyncConfig(buf) }
        )

        fun create(): ClientMessageSyncConfig =
            ClientMessageSyncConfig(encodeConfigSnapshot())

        @Suppress("UnstableApiUsage")
        fun handle(msg: ClientMessageSyncConfig, ctx: IPayloadContext) {
            ctx.enqueueWork {
                val player = ctx.player() as? ServerPlayer ?: return@enqueueWork
                if (!ConfigManager.canUpdateServerConfig(player)) return@enqueueWork
                val backup = encodeConfigSnapshot()
                val incoming = FriendlyByteBuf(Unpooled.wrappedBuffer(msg.payload))
                try {
                    Config.decode(incoming)
                    Config.sync(ESyncDirection.CLIENT_TO_SERVER)
                    Config.saveToFile()
                } catch (t: Throwable) {
                    restoreSnapshot(backup)
                    TaCZTweaks.LOGGER.warn("Rejected invalid synced config payload from {}: {}", player.scoreboardName, t.message)
                    return@enqueueWork
                } finally {
                    incoming.release()
                }

                AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()
                    ?.let { NetworkHandler.sendSyncConfigAll(it) }
            }
        }

        private fun readPayload(buf: FriendlyByteBuf): ByteArray {
            val size = buf.readInt()
            require(size in 0..MAX_BYTES) { "config payload exceeds $MAX_BYTES bytes: $size" }
            val bytes = ByteArray(size)
            buf.readBytes(bytes)
            return bytes
        }

        private fun encodeConfigSnapshot(): ByteArray {
            val buf = FriendlyByteBuf(Unpooled.buffer())
            return try {
                Config.encode(buf)
                val size = buf.writerIndex()
                require(size in 0..MAX_BYTES) { "encoded config exceeds $MAX_BYTES bytes: $size" }
                ByteArray(size).also { buf.getBytes(0, it) }
            } finally {
                buf.release()
            }
        }

        private fun restoreSnapshot(snapshot: ByteArray) {
            val restore = FriendlyByteBuf(Unpooled.wrappedBuffer(snapshot))
            try {
                Config.decode(restore)
                Config.sync(ESyncDirection.CLIENT_TO_SERVER)
            } finally {
                restore.release()
            }
        }
    }
}
