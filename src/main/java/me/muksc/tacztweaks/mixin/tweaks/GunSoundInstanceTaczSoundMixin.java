package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import me.muksc.tacztweaks.mixininterface.tweaks.MonoTaczSound;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * When TaCZ builds a mono {@code TaczSound}, remember its path so
 * {@link SoundBufferLibraryMixin} can downmix the stereo ogg.
 */
@Mixin(targets = "com.tacz.guns.client.sound.GunSoundInstance$TaczSound", remap = false)
public abstract class GunSoundInstanceTaczSoundMixin implements MonoTaczSound {
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

    @ModifyReturnValue(method = "getPath", at = @At("RETURN"), remap = true)
    private Identifier tacztweaks$getPath$markMono(Identifier original) {
        if (tacztweaks$mono && original != null) MonoConversion.INSTANCE.mark(original);
        return original;
    }
}
