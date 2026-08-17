package me.muksc.tacztweaks.mixin.modifiers;

import com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = AmmoSpeedModifier.class, remap = false)
public abstract class AmmoSpeedModifierMixin {
    @ModifyArg(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/modifier/CacheValue;<init>(Ljava/lang/Object;)V"), index = 0)
    private Object tacztweaks$initCache$speedModifier(Object value) {
        if (!(value instanceof Float speed)) return value;
        return (float) Config.Modifiers.Speed.INSTANCE.eval(speed);
    }
}
