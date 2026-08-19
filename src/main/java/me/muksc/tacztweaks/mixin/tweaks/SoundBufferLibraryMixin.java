package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.sugar.Local;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import net.minecraft.client.sounds.FiniteAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * 1.21.11 moved the complete-buffer decode into a nested supplyAsync lambda; the actual PCM buffer
 * and AudioFormat are still both handed to the SoundBuffer constructor in one place, so we can
 * source-verify and patch the downmix there.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @SuppressWarnings("target")
    @ModifyArg(
        method = "lambda$getCompleteBuffer$1(Lnet/minecraft/resources/Identifier;)Lcom/mojang/blaze3d/audio/SoundBuffer;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(Ljava/nio/ByteBuffer;Ljavax/sound/sampled/AudioFormat;)V"
        ),
        index = 1
    )
    private AudioFormat tacztweaks$getCompleteBuffer$monoFormat(AudioFormat format, @Local(argsOnly = true) Identifier id) {
        return MonoConversion.INSTANCE.shouldConvert(format, id) ? MonoConversion.INSTANCE.convertFormat(format) : format;
    }

    @SuppressWarnings("target")
    @ModifyArg(
        method = "lambda$getCompleteBuffer$1(Lnet/minecraft/resources/Identifier;)Lcom/mojang/blaze3d/audio/SoundBuffer;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(Ljava/nio/ByteBuffer;Ljavax/sound/sampled/AudioFormat;)V"
        ),
        index = 0
    )
    private ByteBuffer tacztweaks$getCompleteBuffer$monoBuffer(
        ByteBuffer data,
        @Local(argsOnly = true) Identifier id,
        @Local FiniteAudioStream as
    ) {
        AudioFormat rawFormat = as.getFormat();
        return MonoConversion.INSTANCE.shouldConvert(rawFormat, id)
            ? MonoConversion.INSTANCE.convertData(data, rawFormat)
            : data;
    }
}
