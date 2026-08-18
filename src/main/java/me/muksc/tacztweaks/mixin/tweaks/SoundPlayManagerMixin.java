package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.sound.SoundManager;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.network.NetworkHandler;
import me.muksc.tacztweaks.network.message.ClientMessageBroadcastSound;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sound tweaks:
 * <ul>
 *   <li>suppress headshot / flesh-hit / kill sounds;</li>
 *   <li>force the default hit/kill sounds regardless of the gun pack's own sounds;</li>
 *   <li>broadcast first-person gun sounds (reload / inspect / bolt / draw / put-away /
 *       fire-select / melee / dry-fire) to other players in multiplayer.</li>
 * </ul>
 */
@Mixin(value = SoundPlayManager.class, remap = false)
public abstract class SoundPlayManagerMixin {
    @Inject(method = "clearSoundResourceCache", at = @At("HEAD"))
    private static void tacztweaks$clearSoundResourceCache$clearMonoPaths(CallbackInfo ci) {
        MonoConversion.INSTANCE.clear();
    }

    // ---- suppress hit/kill sounds ----
    @WrapWithCondition(method = "playHeadHitSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFIZIZZ)Lcom/tacz/guns/client/sound/GunSoundInstance;"))
    private static boolean tacztweaks$playHeadHitSound$conditional(Entity entity, Identifier name, float volume, float pitch, int distance, boolean mono, int concurrencyLimit, boolean trackEntity, boolean relative) {
        return !Config.Tweaks.INSTANCE.suppressHeadHitSounds();
    }

    @WrapWithCondition(method = "playFleshHitSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFIZIZZ)Lcom/tacz/guns/client/sound/GunSoundInstance;"))
    private static boolean tacztweaks$playFleshHitSound$conditional(Entity entity, Identifier name, float volume, float pitch, int distance, boolean mono, int concurrencyLimit, boolean trackEntity, boolean relative) {
        return !Config.Tweaks.INSTANCE.suppressFleshHitSounds();
    }

    @WrapWithCondition(method = "playKillSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFI)Lcom/tacz/guns/client/sound/GunSoundInstance;"))
    private static boolean tacztweaks$playKillSound$conditional(Entity entity, Identifier name, float volume, float pitch, int distance) {
        return !Config.Tweaks.INSTANCE.suppressKillSounds();
    }

    // First-person gunshot assets are usually stereo. When the server substitutes one
    // for a positional 3P sound, ask GunSoundInstance to load it as mono as well.
    @WrapOperation(
        method = "playMessageSound",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z")
    )
    private static boolean tacztweaks$playMessageSound$monoFirstPersonSounds(
        String expected,
        Object soundName,
        Operation<Boolean> original
    ) {
        boolean result = original.call(expected, soundName);
        if (!Config.Tweaks.INSTANCE.betterMonoConversion()) return result;
        return result || SoundManager.SHOOT_SOUND.equals(soundName) || SoundManager.SILENCE_SOUND.equals(soundName);
    }

    // ---- force default hit/kill sounds ----
    @ModifyExpressionValue(method = "playHeadHitSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lnet/minecraft/resources/Identifier;"))
    private static Identifier tacztweaks$playHeadHitSounds$forceDefaultSound(Identifier original) {
        return original != null && Config.Tweaks.INSTANCE.forceDefaultHitAndKillSounds()
            ? Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "head_hit")
            : original;
    }

    @ModifyExpressionValue(method = "playFleshHitSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lnet/minecraft/resources/Identifier;"))
    private static Identifier tacztweaks$playFleshHitSounds$forceDefaultSound(Identifier original) {
        return original != null && Config.Tweaks.INSTANCE.forceDefaultHitAndKillSounds()
            ? Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "flesh_hit")
            : original;
    }

    @ModifyExpressionValue(method = "playKillSound", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lnet/minecraft/resources/Identifier;"))
    private static Identifier tacztweaks$playKillSounds$forceDefaultSound(Identifier original) {
        return original != null && Config.Tweaks.INSTANCE.forceDefaultHitAndKillSounds()
            ? Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "kill")
            : original;
    }

    // ---- broadcast first-person gun sounds to other players ----
    private static final String PC5 = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/Identifier;FFI)Lcom/tacz/guns/client/sound/GunSoundInstance;";

    @WrapOperation(method = "playDryFireSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playDryFireSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playReloadSound", at = @At(value = "INVOKE", target = PC5, ordinal = 0))
    private static GunSoundInstance tacztweaks$playReloadSound$broadcast0(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playReloadSound", at = @At(value = "INVOKE", target = PC5, ordinal = 1))
    private static GunSoundInstance tacztweaks$playReloadSound$broadcast1(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playInspectSound", at = @At(value = "INVOKE", target = PC5, ordinal = 0))
    private static GunSoundInstance tacztweaks$playInspectSound$broadcast0(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playInspectSound", at = @At(value = "INVOKE", target = PC5, ordinal = 1))
    private static GunSoundInstance tacztweaks$playInspectSound$broadcast1(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playBoltSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playBoltSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playDrawSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playDrawSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playPutAwaySound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playPutAwaySound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playFireSelectSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playFireSelectSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playMeleeBayonetSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playMeleeBayonetSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playMeleePushSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playMeleePushSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playMeleeStockSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playMeleeStockSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @WrapOperation(method = "playAnimationSound", at = @At(value = "INVOKE", target = PC5))
    private static GunSoundInstance tacztweaks$playAnimationSound$broadcast(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        return tacztweaks$broadcastSound(entity, name, volume, pitch, distance, original);
    }

    @Unique
    private static GunSoundInstance tacztweaks$broadcastSound(Entity entity, Identifier name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        if (Config.Tweaks.INSTANCE.audibleFirstPersonGunSounds() && name != null) NetworkHandler.INSTANCE.sendC2S(new ClientMessageBroadcastSound(name, volume, pitch, distance));
        return original.call(entity, name, volume, pitch, distance);
    }
}
