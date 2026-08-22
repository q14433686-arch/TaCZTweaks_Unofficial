package me.muksc.tacztweaks.network.message

import io.netty.buffer.Unpooled
import io.netty.handler.codec.DecoderException
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.network.ClientPacketBridge
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

class ServerMessageSyncConfig private constructor(val buf: FriendlyByteBuf) : CustomPacketPayload {
    fun write(out: FriendlyByteBuf) {
        val length = buf.readableBytes()
        require(length <= MAX_CONFIG_BYTES) { "Config payload is too large: $length" }
        out.writeInt(length)
        out.writeBytes(buf, buf.readerIndex(), length)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        private const val MAX_CONFIG_BYTES = 1 shl 20
        val TYPE = CustomPacketPayload.Type<ServerMessageSyncConfig>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "server_sync_config")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ServerMessageSyncConfig> = StreamCodec.ofMember(
            ServerMessageSyncConfig::write,
            { source ->
                val length = source.readInt()
                if (length !in 0..MAX_CONFIG_BYTES || length > source.readableBytes()) {
                    throw DecoderException("Invalid TaCZ Tweaks config payload length: $length")
                }
                ServerMessageSyncConfig(FriendlyByteBuf(source.readBytes(length)))
            }
        )

        fun create(): ServerMessageSyncConfig =
            ServerMessageSyncConfig(FriendlyByteBuf(Unpooled.buffer()).also { Config.encode(it) })

        fun handle(msg: ServerMessageSyncConfig, ctx: IPayloadContext) {
            ctx.enqueueWork {
                ClientPacketBridge.invoke(
                    "onSyncConfig",
                    arrayOf<Class<*>>(ServerMessageSyncConfig::class.java),
                    msg
                )
            }
        }
    }
}
