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
 * Captures the (otherwise ignored) {@code mono} constructor argument and
 * forwards it to the {@code TaczSound} created in {@link GunSoundInstance#resolve}.
 */
@Mixin(value = GunSoundInstance.class, remap = false)
public abstract class GunSoundInstanceMixin {
    @Unique
    private boolean tacztweaks$mono = false;

    @Inject(
        method = "<init>(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/world/entity/Entity;ILnet/minecraft/resources/Identifier;ZZ)V",
        at = @At("TAIL")
    )
    private void tacztweaks$init$storeMono(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int soundDistance, Identifier registryName, boolean mono, boolean relative, CallbackInfo ci) {
        tacztweaks$mono = mono;
    }

    @ModifyExpressionValue(method = "resolve", at = @At(value = "NEW", target = "com/tacz/guns/client/sound/GunSoundInstance$TaczSound"))
    private @Coerce Object tacztweaks$resolve$setMono(@Coerce Object original) {
        ((MonoTaczSound) original).tacztweaks$setMono(tacztweaks$mono);
        return original;
    }
}
