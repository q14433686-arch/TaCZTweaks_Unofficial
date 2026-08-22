package me.muksc.tacztweaks.network.message
import net.neoforged.neoforge.network.handling.IPayloadContext

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

class ServerMessageSyncConfig private constructor(private val payload: ByteArray) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(readPayload(buf))

    fun write(out: FriendlyByteBuf) {
        out.writeInt(payload.size)
        out.writeBytes(payload)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        private const val MAX_BYTES = 1 shl 20

        val TYPE = CustomPacketPayload.Type<ServerMessageSyncConfig>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "server_sync_config")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ServerMessageSyncConfig> = StreamCodec.ofMember(
            ServerMessageSyncConfig::write,
            { buf -> ServerMessageSyncConfig(buf) }
        )

        fun create(): ServerMessageSyncConfig =
            ServerMessageSyncConfig(encodeConfigSnapshot())

        fun handle(msg: ServerMessageSyncConfig, ctx: IPayloadContext) {
            ctx.enqueueWork {
            val client = Minecraft.getInstance()
            val incoming = FriendlyByteBuf(Unpooled.wrappedBuffer(msg.payload))
            try {
                Config.decode(incoming)
                Config.sync(ESyncDirection.SERVER_TO_CLIENT)
                client.player?.also { player ->
                    AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
                }
            } catch (t: Throwable) {
                TaCZTweaks.LOGGER.warn("Rejected invalid server config payload: {}", t.message)
            } finally {
                incoming.release()
            }
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
    }
}
