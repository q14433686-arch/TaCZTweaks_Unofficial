package me.muksc.tacztweaks.mixin.tweaks;

import com.mojang.blaze3d.audio.SoundBuffer;
import kotlin.Pair;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * Mutates a freshly-loaded {@link SoundBuffer} from stereo to mixed mono.
 * Field names are Mojang's 26.2 names ({@code data} / {@code format}).
 */
@Mixin(SoundBuffer.class)
public abstract class SoundBufferMixin implements SoundBufferLibraryMixin.SoundBufferExtension {
    @Shadow(aliases = {"sample"})
    private ByteBuffer data;

    @Mutable
    @Shadow(aliases = {"sampleFormat"})
    @Final
    private AudioFormat format;

    @Unique
    private boolean tacztweaks$converted = false;

    @Override
    public void tacztweaks$convertToMixedMono() {
        if (tacztweaks$converted) return;
        if (data == null || format == null) return;
        if (format.getChannels() <= 1) return;
        int bits = format.getSampleSizeInBits();
        if (bits != 16 && bits != 8) return;
        Pair<ByteBuffer, AudioFormat> converted = MonoConversion.INSTANCE.convert(data, format);
        this.data = converted.getFirst();
        this.format = converted.getSecond();
        tacztweaks$converted = true;
    }
}
