package me.muksc.tacztweaks.mixin.modifiers;

import com.tacz.guns.resource.modifier.custom.HeadShotModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = HeadShotModifier.class, remap = false)
public abstract class HeadshotModifierMixin {
    @ModifyArg(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/modifier/CacheValue;<init>(Ljava/lang/Object;)V"), index = 0)
    private Object tacztweaks$initCache$headshotModifier(Object value) {
        if (!(value instanceof Float headshot)) return value;
        return (float) Config.Modifiers.Headshot.INSTANCE.eval(headshot);
    }
}
