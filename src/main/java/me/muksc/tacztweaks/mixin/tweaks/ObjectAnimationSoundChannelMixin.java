package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.client.animation.ObjectAnimationSoundChannel;
import com.tacz.guns.client.sound.GunSoundInstance;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.network.NetworkHandler;
import me.muksc.tacztweaks.network.message.ClientMessageBroadcastSound;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Broadcasts first-person keyframe sounds before TaCZ submits them to its sound manager. */
@Mixin(value = ObjectAnimationSoundChannel.class, remap = false)
public abstract class ObjectAnimationSoundChannelMixin {
    @WrapOperation(
        method = "playSound(DDLnet/minecraft/world/entity/Entity;IFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playAnimationSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFI)Lcom/tacz/guns/client/sound/GunSoundInstance;"
        )
    )
    private GunSoundInstance tacztweaks$playSound$broadcast(
        Entity entity,
        Identifier name,
        float volume,
        float pitch,
        int distance,
        Operation<GunSoundInstance> original
    ) {
        if (Config.Tweaks.INSTANCE.audibleFirstPersonGunSounds() && name != null) {
            NetworkHandler.INSTANCE.sendC2S(new ClientMessageBroadcastSound(name, volume, pitch, distance));
        }
        return original.call(entity, name, volume, pitch, distance);
    }
}
