package me.muksc.tacztweaks.mixin.tweaks;

import me.muksc.tacztweaks.client.sound.MonoConversion;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * Downmixes a TaCZ sound while vanilla constructs its {@code SoundBuffer}.
 *
 * <p>This operates before the buffer is cached or uploaded to OpenAL. Mutating a completed
 * {@code SoundBuffer} is unsafe in 26.2: besides its data and format, it caches a final byte
 * count and may already own an OpenAL buffer. Modifying both constructor arguments at once
 * keeps those values internally consistent and avoids depending on lambda local-variable
 * table types.</p>
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @ModifyArgs(
        method = "lambda$getCompleteBuffer$1(Lnet/minecraft/resources/Identifier;)Lcom/mojang/blaze3d/audio/SoundBuffer;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(Ljava/nio/ByteBuffer;Ljavax/sound/sampled/AudioFormat;)V"
        )
    )
    private void tacztweaks$getCompleteBuffer$convert(Args args, Identifier id) {
        ByteBuffer data = args.get(0);
        AudioFormat format = args.get(1);
        if (!MonoConversion.INSTANCE.shouldConvert(format, id)) return;
        args.set(0, MonoConversion.INSTANCE.convertData(data, format));
        args.set(1, MonoConversion.INSTANCE.convertFormat(format));
    }
}
