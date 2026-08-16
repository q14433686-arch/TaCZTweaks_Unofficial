package me.muksc.tacztweaks.mixin.feature.audio_and_visuals.audio.force_first_person_shooting_sound;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.message.ServerMessageSound;
import com.tacz.guns.sound.SoundManager;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundPlayManager.class, remap = false)
public abstract class SoundPlayManagerMixin {
    //? if >=1.21.11 {
    /*@Inject(method = "playMessageSound", at = @At("HEAD"), cancellable = true)
    private static void tacztweaks$playMessageSound$forceFirstPersonShootingSound(ServerMessageSound message, CallbackInfo ci) {
        if (!Config.AudioAndVisuals.Audio.forceFirstPersonShootingSound()) return;
        String soundName = message.getSoundName();
        if (!SoundManager.SHOOT_SOUND.equals(soundName) && !SoundManager.SILENCE_SOUND.equals(soundName)) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(message.getEntityId()) instanceof LivingEntity livingEntity)) return;
        TimelessAPI.getGunDisplay(message.getGunDisplayId(), message.getGunId()).ifPresent(display -> {
            var soundId = display.getSounds(soundName);
            if (soundId == null) return;
            // The public mono overload is stable; unlike the original 1P branch it follows the
            // entity and therefore remains audible to nearby clients receiving the broadcast.
            SoundPlayManager.playClientSound(
                livingEntity, soundId, message.getVolume(), message.getPitch(), message.getDistance(), true
            );
            ci.cancel();
        });
    }
    *///?} else {
    @Definition(id = "SHOOT_3P_SOUND", field = "Lcom/tacz/guns/sound/SoundManager;SHOOT_3P_SOUND:Ljava/lang/String;")
    @Definition(id = "equals", method = "Ljava/lang/String;equals(Ljava/lang/Object;)Z")
    @Expression("SHOOT_3P_SOUND.equals(?)")
    @WrapOperation(method = "lambda$playMessageSound$1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean tacztweaks$playMessageSound$forceFirstPersonShootingSound$mono(String instance, Object anObject, Operation<Boolean> original) {
        if (!Config.AudioAndVisuals.Audio.forceFirstPersonShootingSound()) return original.call(instance, anObject);
        return original.call(instance, anObject)
            || SoundManager.SHOOT_SOUND.equals(anObject)
            || SoundManager.SILENCE_SOUND.equals(anObject);
    }
    //?}
}
