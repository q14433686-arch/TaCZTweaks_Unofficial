package me.muksc.tacztweaks.compat.soundphysics.network.message

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.network.ClientPacketBridge
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

object ServerMessageSoundPhysicsRequired : CustomPacketPayload {
    val TYPE = CustomPacketPayload.Type<ServerMessageSoundPhysicsRequired>(
        Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "server_sound_physics_required")
    )
    val CODEC: StreamCodec<FriendlyByteBuf, ServerMessageSoundPhysicsRequired> =
        StreamCodec.unit(ServerMessageSoundPhysicsRequired)

    fun handle(msg: ServerMessageSoundPhysicsRequired, ctx: IPayloadContext) {
        ctx.enqueueWork {
            ClientPacketBridge.invoke(
                "onSoundPhysicsRequired",
                arrayOf<Class<*>>(ServerMessageSoundPhysicsRequired::class.java),
                msg
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
