package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.SoundBuffer;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @Unique
    private final Map<Identifier, CompletableFuture<SoundBuffer>> tacztweaks$monoCache = new ConcurrentHashMap<>();

    @Invoker("lambda$getCompleteBuffer$1")
    protected abstract SoundBuffer tacztweaks$loadCompleteBuffer(Identifier id);

    @Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$getCompleteBuffer$selectVariant(Identifier id, CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir) {
        if (!MonoConversion.INSTANCE.consumeMonoRequest(id)) return;
        cir.setReturnValue(tacztweaks$monoCache.computeIfAbsent(id, key ->
            CompletableFuture.supplyAsync(() -> {
                MonoConversion.INSTANCE.beginConversion(key);
                try {
                    return tacztweaks$loadCompleteBuffer(key);
                } finally {
                    MonoConversion.INSTANCE.endConversion();
                }
            }, Util.nonCriticalIoPool())
        ));
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void tacztweaks$clear$discardMonoCache(CallbackInfo ci) {
        tacztweaks$monoCache.values().forEach(future -> future.thenAccept(SoundBuffer::discardAlBuffer));
        tacztweaks$monoCache.clear();
        MonoConversion.INSTANCE.clear();
    }

    @ModifyArgs(
        method = "lambda$getCompleteBuffer$1(Lnet/minecraft/resources/Identifier;)Lcom/mojang/blaze3d/audio/SoundBuffer;",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/SoundBuffer;<init>(Ljava/nio/ByteBuffer;Ljavax/sound/sampled/AudioFormat;)V")
    )
    private void tacztweaks$getCompleteBuffer$convert(Args args, @Local(argsOnly = true) Identifier id) {
        ByteBuffer data = args.get(0);
        AudioFormat format = args.get(1);
        if (!MonoConversion.INSTANCE.shouldConvert(format, id)) return;
        args.set(0, MonoConversion.INSTANCE.convertData(data, format));
        args.set(1, MonoConversion.INSTANCE.convertFormat(format));
    }
}
