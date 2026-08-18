package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.sugar.Local;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.mixininterface.tweaks.MonoAudio;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Downmix stereo gun sounds to mono by averaging channels instead of dropping one.
 * Several method names are listed because 26.2 may have renamed the 1.20 lambda.
 * {@code require = 0} so a missing name does not crash mixin apply.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    private static final String SOUND_BUFFER_INIT = "Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(Ljava/nio/ByteBuffer;Ljavax/sound/sampled/AudioFormat;)V";

    @ModifyArg(method = {"getCompleteBuffer", "lambda$getCompleteBuffer$0", "loadCompleteBuffer"}, at = @At(value = "INVOKE", target = SOUND_BUFFER_INIT), index = 1, require = 0)
    private AudioFormat tacztweaks$monoFrameSize(AudioFormat format, @Local(argsOnly = true) Identifier id) {
        if (!shouldConvert(format, id)) return format;
        return new AudioFormat(
            format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(),
            1, format.getFrameSize() / 2,
            format.getFrameRate(), format.isBigEndian(), format.properties()
        );
    }

    @ModifyArg(method = {"getCompleteBuffer", "lambda$getCompleteBuffer$0", "loadCompleteBuffer"}, at = @At(value = "INVOKE", target = SOUND_BUFFER_INIT), index = 0, require = 0)
    private ByteBuffer tacztweaks$monoBuffer(ByteBuffer data, @Local(argsOnly = true) Identifier id) {
        if (!Config.Tweaks.INSTANCE.betterMonoConversion()) return data;
        if (!MonoAudio.isMarked(id)) return data;
        // format is not available here; convert assuming 16-bit stereo if remaining is even
        if (data.remaining() < 4 || (data.remaining() % 4) != 0) return data;

        ByteBuffer mono = ByteBuffer.allocateDirect(data.remaining() / 2);
        mono.order(data.order());
        ShortBuffer stereo = data.asShortBuffer();
        while (stereo.remaining() >= 2) {
            short left = stereo.get();
            short right = stereo.get();
            mono.putShort((short) ((left + right) / 2));
        }
        mono.flip();
        return mono;
    }

    @Unique
    private static boolean shouldConvert(AudioFormat format, Identifier id) {
        if (!Config.Tweaks.INSTANCE.betterMonoConversion()) return false;
        if (!MonoAudio.isMarked(id)) return false;
        if (format.getChannels() == 1) return false;
        int bits = format.getSampleSizeInBits();
        return bits == 16 || bits == 8;
    }
}
