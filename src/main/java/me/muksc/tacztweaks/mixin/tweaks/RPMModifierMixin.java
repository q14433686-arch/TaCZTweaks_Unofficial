package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.resource.modifier.custom.RpmModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces the RPM (rounds per minute) stat with RPS (rounds per second) in the
 * gun smith table diagrams, when the {@code rps} tweak is enabled.
 */
@Mixin(value = RpmModifier.class, remap = false)
public abstract class RPMModifierMixin {
    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getRoundsPerMinute(Lcom/tacz/guns/api/item/gun/FireMode;)I"))
    private int tacztweaks$getPropertyDiagramsData$rps$original(int original) {
        return Config.Tweaks.INSTANCE.rps() ? original / 60 : original;
    }

    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentCacheProperty;getCache(Ljava/lang/String;)Ljava/lang/Object;"))
    private Object tacztweaks$getPropertyDiagramsData$rps$modified(Object original) {
        if (!Config.Tweaks.INSTANCE.rps()) return original;
        if (original instanceof Integer i) return i / 60;
        return original;
    }

    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "CONSTANT", args = "doubleValue=1200.0", ordinal = 0))
    private double tacztweaks$getPropertyDiagramsData$divisor0(double original) {
        return Config.Tweaks.INSTANCE.rps() ? original / 60 : original;
    }

    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "CONSTANT", args = "doubleValue=1200.0", ordinal = 1))
    private double tacztweaks$getPropertyDiagramsData$divisor1(double original) {
        return Config.Tweaks.INSTANCE.rps() ? original / 60 : original;
    }

    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "CONSTANT", args = "stringValue=gui.tacz.gun_refit.property_diagrams.rpm"))
    private String tacztweaks$getPropertyDiagramsData$rps$diagram(String original) {
        return Config.Tweaks.INSTANCE.rps() ? "tacztweaks.property_diagrams.rps" : original;
    }

    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "CONSTANT", args = "stringValue=%drpm §a(+%d)"))
    private String tacztweaks$getPropertyDiagramsData$rps$positive(String original) {
        return Config.Tweaks.INSTANCE.rps() ? "%drps §a(+%d)" : original;
    }

    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "CONSTANT", args = "stringValue=%drpm §c(%d)"))
    private String tacztweaks$getPropertyDiagramsData$rps$negative(String original) {
        return Config.Tweaks.INSTANCE.rps() ? "%drps §c(%d)" : original;
    }

    @ModifyExpressionValue(method = "getPropertyDiagramsData", at = @At(value = "CONSTANT", args = "stringValue=%drpm"))
    private String tacztweaks$getPropertyDiagramsData$rps$default(String original) {
        return Config.Tweaks.INSTANCE.rps() ? "%drps" : original;
    }
}
