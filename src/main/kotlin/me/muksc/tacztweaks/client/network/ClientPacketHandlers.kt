package me.muksc.tacztweaks.client.network

import com.tacz.guns.client.sound.SoundPlayManager
import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import io.netty.buffer.Unpooled
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageAirspaceSounds
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageSoundPhysicsRequired
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.config.ConfigManager
import me.muksc.tacztweaks.config.sync.ESyncDirection
import me.muksc.tacztweaks.network.message.ServerMessageBroadcastSound
import me.muksc.tacztweaks.network.message.ServerMessageSyncConfig
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.client.network.ClientPacketDistributor

/** Client-only payload work reached through the dedicated-safe reflection bridge. */
object ClientPacketHandlers {

    @JvmStatic
    fun sendToServer(payload: CustomPacketPayload) {
        ClientPacketDistributor.sendToServer(payload)
    }

    @JvmStatic
    fun onBroadcastSound(msg: ServerMessageBroadcastSound) {
        val entity = Minecraft.getInstance().level?.getEntity(msg.entityId) ?: return
        SoundPlayManager.playClientSound(
            entity,
            msg.soundName,
            msg.volume,
            msg.pitch,
            msg.distance,
            true
        )
    }

    @JvmStatic
    fun onSyncConfig(msg: ServerMessageSyncConfig) {
        val backup = FriendlyByteBuf(Unpooled.buffer()).also { Config.encode(it) }
        try {
            Config.decode(msg.buf)
            Config.sync(ESyncDirection.SERVER_TO_CLIENT)
            ConfigManager.syncedWithServer = true
            Minecraft.getInstance().player?.also { player ->
                AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
            }
        } catch (error: RuntimeException) {
            backup.readerIndex(0)
            Config.decode(backup)
            TaCZTweaks.LOGGER.error("Received an invalid config payload from the server", error)
        } finally {
            backup.release()
            msg.buf.release()
        }
    }

    @JvmStatic
    fun onAirspaceSounds(msg: ServerMessageAirspaceSounds) {
        if (!SoundPhysicsCompat.isEnabled()) return
        SoundPhysicsCompat.play(Minecraft.getInstance(), msg)
    }

    @JvmStatic
    fun onSoundPhysicsRequired(msg: ServerMessageSoundPhysicsRequired) {
        if (SoundPhysicsCompat.isEnabled()) return
        // 26.2 removed Gui#chat and LocalPlayer#displayClientMessage.
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("[TaCZ Tweaks] ").withStyle(ChatFormatting.GOLD)
                .append(
                    TaCZTweaks.translatable("bullet_sounds.sound_physics_missing")
                        .withStyle(ChatFormatting.YELLOW)
                )
        )
    }
}
