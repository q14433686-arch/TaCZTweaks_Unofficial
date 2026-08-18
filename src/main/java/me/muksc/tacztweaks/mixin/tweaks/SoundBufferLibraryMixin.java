package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.SoundBuffer;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.mixin.accessor.SoundBufferAccessor;
import me.muksc.tacztweaks.mixininterface.tweaks.TaCZIdentifier;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Port of upstream {@code SoundBufferLibraryMixin} for {@code betterMonoConversion}.
 *
 * <p>Upstream hooked {@code lambda$getCompleteBuffer$0(ResourceLocation)} because that
 * is where the decoded bytes were wrapped into a {@link SoundBuffer}. In 26.2 the
 * buffer creation moved into a nested async lambda, so instead we wrap the whole
 * {@code getCompleteBuffer(Identifier)} result with {@code thenApply(...)} and
 * down-mix the produced buffer. This avoids any dependence on javac lambda names.</p>
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @ModifyReturnValue(method = "getCompleteBuffer", at = @At("RETURN"))
    private CompletableFuture<SoundBuffer> tacztweaks$getCompleteBuffer$mono(
        CompletableFuture<SoundBuffer> original,
        @Local(argsOnly = true) Identifier location
    ) {
        if (!Config.Tweaks.INSTANCE.betterMonoConversion()) return original;
        if (!((TaCZIdentifier) location).tacztweaks$getMonoAudio()) return original;
        return original.thenApply(SoundBufferLibraryMixin::tacztweaks$downmixToMono);
    }

    @Unique
    private static SoundBuffer tacztweaks$downmixToMono(SoundBuffer buffer) {
        ByteBuffer data = ((SoundBufferAccessor) buffer).getData();
        if (data == null) return buffer; // already uploaded to OpenAL; leave untouched
        AudioFormat format = buffer.format();
        if (format.getChannels() == 1) return buffer; // already mono
        int sampleSizeInBits = format.getSampleSizeInBits();
        if (sampleSizeInBits != 16 && sampleSizeInBits != 8) return buffer;

        ByteBuffer monoBuffer = ByteBuffer.allocateDirect(data.remaining() / 2);
        monoBuffer.order(data.order());
        if (sampleSizeInBits == 16) {
            ShortBuffer stereo = data.asShortBuffer();
            while (stereo.hasRemaining()) {
                short left = stereo.get();
                short right = stereo.get();
                monoBuffer.putShort((short) ((left + right) / 2));
            }
        } else {
            ByteBuffer src = data.duplicate();
            while (src.hasRemaining()) {
                byte left = src.get();
                byte right = src.get();
                monoBuffer.put((byte) ((left + right) / 2));
            }
        }
        monoBuffer.flip();

        AudioFormat monoFormat = new AudioFormat(
            format.getEncoding(),
            format.getSampleRate(),
            sampleSizeInBits,
            1,
            format.getFrameSize() / 2,
            format.getFrameRate(),
            format.isBigEndian(),
            format.properties()
        );
        return new SoundBuffer(monoBuffer, monoFormat);
    }
}
