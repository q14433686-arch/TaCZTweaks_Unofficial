package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.client.sound.GunSoundInstance;
import me.muksc.tacztweaks.mixininterface.tweaks.MonoTaczSound;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remembers the {@code mono} flag from TaCZ's gun-sound constructors and copies it onto
 * the inner {@code TaczSound} created in {@code resolve()}. Combined with
 * {@link SoundBufferLibraryMixin} this is the 26.2 replacement for marking
 * {@code Identifier} itself.
 */
@Mixin(value = GunSoundInstance.class, remap = false)
public abstract class GunSoundInstanceMixin {
    @Unique
    private boolean tacztweaks$mono = false;

    @Inject(method = "<init>(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/world/entity/Entity;ILnet/minecraft/resources/Identifier;ZZ)V", at = @At("TAIL"))
    private void tacztweaks$init$storeMono(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int soundDistance, Identifier registryName, boolean mono, boolean relative, CallbackInfo ci) {
        tacztweaks$mono = mono;
    }

    @Inject(method = "<init>(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/world/entity/Entity;ILnet/minecraft/resources/Identifier;Z)V", at = @At("TAIL"))
    private void tacztweaks$init$storeMonoShort(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int soundDistance, Identifier registryName, boolean mono, CallbackInfo ci) {
        tacztweaks$mono = mono;
    }

    @ModifyExpressionValue(method = "resolve", at = @At(value = "NEW", target = "com/tacz/guns/client/sound/GunSoundInstance$TaczSound", remap = false), remap = true)
    private @Coerce Object tacztweaks$resolve$setMono(@Coerce Object original) {
        ((MonoTaczSound) original).tacztweaks$setMono(tacztweaks$mono);
        return original;
    }
}
