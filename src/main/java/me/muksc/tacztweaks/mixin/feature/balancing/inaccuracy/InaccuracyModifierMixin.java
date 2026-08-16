package me.muksc.tacztweaks.mixin.feature.balancing.inaccuracy;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = InaccuracyModifier.class, remap = false)
public abstract class InaccuracyModifierMixin {
    /**
     * Transform the completed base cache instead of the implementation-detail forEach lambda.
     * Attachment modifiers are evaluated later, so this has the same ordering as transforming
     * GunData#getInaccuracy immediately before each value is inserted into the cache.
     */
    @ModifyReturnValue(method = "initCache", at = @At("RETURN"))
    private CacheValue<Map<InaccuracyType, Float>> tacztweaks$initCache$inaccuracyModifier(
        CacheValue<Map<InaccuracyType, Float>> original
    ) {
        original.getValue().replaceAll((type, value) -> {
            float inaccuracy = (float) Config.Balancing.Inaccuracy.eval(value);
            return switch (type) {
                case STAND -> (float) Config.Balancing.StandInaccuracy.eval(inaccuracy);
                case AIM -> (float) Config.Balancing.AimInaccuracy.eval(inaccuracy);
                case MOVE -> (float) Config.Balancing.MoveInaccuracy.eval(inaccuracy);
                case SNEAK -> (float) Config.Balancing.SneakInaccuracy.eval(inaccuracy);
                case LIE -> (float) Config.Balancing.CrawlInaccuracy.eval(inaccuracy);
            };
        });
        return original;
    }

    //~ environment environment_client
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @Inject(method = "buildNormal", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentCacheProperty;getCache(Ljava/lang/String;)Ljava/lang/Object;"))
    private void tacztweaks$buildNormal$inaccuracyModifier(
        GunData gunData, AttachmentCacheProperty cacheProperty, GunFireModeAdjustData fireModeAdjustData, InaccuracyType type, String titleKey, double referenceValue, CallbackInfoReturnable<Object> cir,
        @Local(name = "inaccuracy") LocalFloatRef inaccuracyRef
    ) {
        inaccuracyRef.set((float) Config.Balancing.Inaccuracy.eval(inaccuracyRef.get()));
    }

    //~ environment environment_client
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @Inject(method = "buildAim", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentCacheProperty;getCache(Ljava/lang/String;)Ljava/lang/Object;"))
    private void tacztweaks$buildAim$inaccuracyModifier(
        GunData gunData, AttachmentCacheProperty cacheProperty, GunFireModeAdjustData fireModeAdjustData, CallbackInfoReturnable<Object> cir,
        @Local(name = "aimInaccuracy") LocalFloatRef aimInaccuracyRef
    ) {
        float inaccuracy = (float) Config.Balancing.Inaccuracy.eval(1.0F - aimInaccuracyRef.get());
        inaccuracy = (float) Config.Balancing.AimInaccuracy.eval(inaccuracy);
        aimInaccuracyRef.set(1.0F - inaccuracy);
    }
}
