package me.muksc.tacztweaks.mixin.accessor;

import com.mojang.blaze3d.audio.SoundBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.ByteBuffer;

/**
 * Exposes the raw decoded PCM {@link ByteBuffer} of a {@link SoundBuffer} so the
 * mono down-mix can be computed before the buffer is uploaded to OpenAL.
 */
@Mixin(SoundBuffer.class)
public interface SoundBufferAccessor {
    @Accessor("data")
    ByteBuffer getData();
}
