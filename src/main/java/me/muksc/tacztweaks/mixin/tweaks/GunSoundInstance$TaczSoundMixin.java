package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.muksc.tacztweaks.mixininterface.tweaks.MonoTaczSound;
import me.muksc.tacztweaks.mixininterface.tweaks.TaCZIdentifier;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Tags the file {@link Identifier} returned by {@code TaczSound#getPath()} with
 * the mono flag so that {@code SoundBufferLibrary} can down-mix it during decode.
 */
@Mixin(targets = "com.tacz.guns.client.sound.GunSoundInstance$TaczSound", remap = false)
public abstract class GunSoundInstance$TaczSoundMixin implements MonoTaczSound {
    @Unique
    private boolean tacztweaks$mono = false;

    @Override
    public boolean tacztweaks$getMono() {
        return tacztweaks$mono;
    }

    @Override
    public void tacztweaks$setMono(boolean mono) {
        tacztweaks$mono = mono;
    }

    @ModifyReturnValue(method = "getPath", at = @At("RETURN"))
    private Identifier tacztweaks$getPath$tagMono(Identifier original) {
        ((TaCZIdentifier) original).tacztweaks$setMonoAudio(tacztweaks$mono);
        return original;
    }
}
