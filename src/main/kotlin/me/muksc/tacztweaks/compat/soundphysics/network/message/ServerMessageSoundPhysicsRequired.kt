package me.muksc.tacztweaks.compat.soundphysics.network.message
import net.neoforged.neoforge.network.handling.IPayloadContext

import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

object ServerMessageSoundPhysicsRequired : CustomPacketPayload {
    val TYPE = CustomPacketPayload.Type<ServerMessageSoundPhysicsRequired>(
        Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "server_sound_physics_required")
    )
    val CODEC: StreamCodec<FriendlyByteBuf, ServerMessageSoundPhysicsRequired> =
        StreamCodec.unit(ServerMessageSoundPhysicsRequired)

    fun handle(msg: ServerMessageSoundPhysicsRequired, ctx: IPayloadContext) {
        if (SoundPhysicsCompat.isEnabled()) return
        ctx.enqueueWork {
            val client = Minecraft.getInstance()
            client.player?.displayClientMessage(
                Component.literal("[TaCZ Tweaks] ").withStyle(ChatFormatting.GOLD)
                    .append(TaCZTweaks.translatable("bullet_sounds.sound_physics_missing").withStyle(ChatFormatting.YELLOW)),
                false
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
