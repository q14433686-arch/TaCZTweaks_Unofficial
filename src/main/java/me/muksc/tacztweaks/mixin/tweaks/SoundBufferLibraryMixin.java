package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.audio.SoundBuffer;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

/**
 * After vanilla finishes loading an ogg, downmix stereo → mixed mono for sounds TaCZ
 * requested as mono. Done as a future {@code thenApply} so it does not depend on the
 * (version-fragile) lambda name inside {@code getCompleteBuffer}.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @WrapMethod(method = "getCompleteBuffer")
    private CompletableFuture<SoundBuffer> tacztweaks$getCompleteBuffer$convertMono(Identifier id, Operation<CompletableFuture<SoundBuffer>> original) {
        CompletableFuture<SoundBuffer> future = original.call(id);
        if (!Config.Tweaks.INSTANCE.betterMonoConversion()) return future;
        if (id == null || !MonoConversion.INSTANCE.isMarked(id)) return future;
        return future.thenApply(buffer -> {
            if (buffer instanceof SoundBufferExtension ext) ext.tacztweaks$convertToMixedMono();
            return buffer;
        });
    }

    /**
     * Implemented by {@link SoundBufferMixin}. Kept as an inner interface so the wrap
     * above does not need a circular compile-time dependency on the buffer mixin class.
     */
    public interface SoundBufferExtension {
        void tacztweaks$convertToMixedMono();
    }
}
