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
 * Downmixes a TaCZ sound while vanilla constructs its {@code SoundBuffer}.
 *
 * <p>This operates before the buffer is cached or uploaded to OpenAL. Mutating a completed
 * {@code SoundBuffer} is unsafe in 26.2: besides its data and format, it caches a final byte
 * count and may already own an OpenAL buffer. The constructor call in
 * {@code lambda$getCompleteBuffer$1} is a stable, bytecode-audited point where all three
 * values are still consistent.</p>
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @ModifyArg(
        method = "lambda$getCompleteBuffer$1(Lnet/minecraft/resources/Identifier;)Lcom/mojang/blaze3d/audio/SoundBuffer;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(Ljava/nio/ByteBuffer;Ljavax/sound/sampled/AudioFormat;)V"
        ),
        index = 0
    )
    private ByteBuffer tacztweaks$getCompleteBuffer$monoData(
        ByteBuffer original,
        @Local(argsOnly = true) Identifier id,
        @Local FiniteAudioStream stream
    ) {
        AudioFormat format = stream.getFormat();
        if (!MonoConversion.INSTANCE.shouldConvert(format, id)) return original;
        return MonoConversion.INSTANCE.convertData(original, format);
    }

    @ModifyArg(
        method = "lambda$getCompleteBuffer$1(Lnet/minecraft/resources/Identifier;)Lcom/mojang/blaze3d/audio/SoundBuffer;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(Ljava/nio/ByteBuffer;Ljavax/sound/sampled/AudioFormat;)V"
        ),
        index = 1
    )
    private AudioFormat tacztweaks$getCompleteBuffer$monoFormat(
        AudioFormat original,
        @Local(argsOnly = true) Identifier id
    ) {
        if (!MonoConversion.INSTANCE.shouldConvert(original, id)) return original;
        return MonoConversion.INSTANCE.convertFormat(original);
    }
}
