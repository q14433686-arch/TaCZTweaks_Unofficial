package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Common cache hook; client-only diagram helpers live in InaccuracyModifierDiagramMixin. */
@Mixin(value = InaccuracyModifier.class, remap = false)
public abstract class InaccuracyModifierMixin {
    @ModifyExpressionValue(method = "lambda$initCache$0", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getInaccuracy(Lcom/tacz/guns/resource/pojo/data/gun/InaccuracyType;F)F"))
    private static float tacztweaks$initCache$inaccuracyModifier(float original, @Local(argsOnly = true) InaccuracyType type) {
        float inaccuracy = (float) Config.Modifiers.Inaccuracy.INSTANCE.eval(original);
        switch (type) {
            case STAND -> inaccuracy = (float) Config.Modifiers.StandInaccuracy.INSTANCE.eval(inaccuracy);
            case AIM -> inaccuracy = (float) Config.Modifiers.AimInaccuracy.INSTANCE.eval(inaccuracy);
            case MOVE -> inaccuracy = (float) Config.Modifiers.MoveInaccuracy.INSTANCE.eval(inaccuracy);
            case SNEAK -> inaccuracy = (float) Config.Modifiers.SneakInaccuracy.INSTANCE.eval(inaccuracy);
            case LIE -> inaccuracy = (float) Config.Modifiers.CrawlInaccuracy.INSTANCE.eval(inaccuracy);
        }
        return inaccuracy;
    }
}
