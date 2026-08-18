package me.muksc.tacztweaks.compat.soundphysics.network.message

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

    fun handle(msg: ServerMessageSoundPhysicsRequired, client: Minecraft) {
        if (SoundPhysicsCompat.isEnabled()) return
        client.execute {
            // 26.2: Gui#chat and LocalPlayer#displayClientMessage are gone.
            // TaCZ itself uses Player#sendSystemMessage(Component) on the client.
            client.player?.sendSystemMessage(
                Component.literal("[TaCZ Tweaks] ").withStyle(ChatFormatting.GOLD)
                    .append(TaCZTweaks.translatable("bullet_sounds.sound_physics_missing").withStyle(ChatFormatting.YELLOW))
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
