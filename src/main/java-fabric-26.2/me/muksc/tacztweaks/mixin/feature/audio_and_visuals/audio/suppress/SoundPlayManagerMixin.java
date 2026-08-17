package me.muksc.tacztweaks.mixin.feature.audio_and_visuals.audio.suppress;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SoundPlayManager.class, remap = false)
public abstract class SoundPlayManagerMixin {
    @WrapOperation(method = "playHeadHitSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFIZIZZ)Lcom/tacz/guns/client/sound/GunSoundInstance;"))
    private static GunSoundInstance tacztweaks$playHeadHitSound$suppressHeadshotSounds(Entity entity, Identifier name, float volume, float pitch, int distance, boolean mono, int concurrencyLimit, boolean trackEntity, boolean relative, Operation<GunSoundInstance> original) {
        return Config.AudioAndVisuals.Audio.suppressHeadshotSounds()
            ? null
            : original.call(entity, name, volume, pitch, distance, mono, concurrencyLimit, trackEntity, relative);
    }

    @WrapOperation(method = "playFleshHitSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFIZIZZ)Lcom/tacz/guns/client/sound/GunSoundInstance;"))
    private static GunSoundInstance tacztweaks$playFleshHitSound$suppressHitSounds(Entity entity, Identifier name, float volume, float pitch, int distance, boolean mono, int concurrencyLimit, boolean trackEntity, boolean relative, Operation<GunSoundInstance> original) {
        return Config.AudioAndVisuals.Audio.suppressHitSounds()
            ? null
            : original.call(entity, name, volume, pitch, distance, mono, concurrencyLimit, trackEntity, relative);
    }

    @WrapOperation(method = "playKillSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFI)Lcom/tacz/guns/client/sound/GunSoundInstance;"))
    private static GunSoundInstance tacztweaks$playKillSound$suppressKillSounds(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return Config.AudioAndVisuals.Audio.suppressKillSounds()
            ? null
            : original.call(entity, name, volume, pitch, distance);
    }
}
