package me.muksc.tacztweaks.mixin.modifiers;

import com.tacz.guns.resource.modifier.custom.ArmorIgnoreModifier;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Common cache hook; client-only property diagrams live in ArmorIgnoreModifierDiagramMixin. */
@Mixin(value = ArmorIgnoreModifier.class, remap = false)
public abstract class ArmorIgnoreModifierMixin {
    @ModifyArg(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/modifier/CacheValue;<init>(Ljava/lang/Object;)V"), index = 0)
    private Object tacztweaks$initCache$armorIgnoreModifier(Object value) {
        if (!(value instanceof Float armorIgnore)) return value;
        return (float) Config.Modifiers.ArmorIgnore.INSTANCE.eval(armorIgnore);
    }
}
