package me.muksc.tacztweaks.mixin.modifiers;

import com.tacz.guns.util.AttachmentDataUtils;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Global multipliers for the damage / headshot / armor-ignore values computed by
 * {@link AttachmentDataUtils} (used by tooltips, the gun smith table, etc.).
 */
@Mixin(value = AttachmentDataUtils.class, remap = false)
public abstract class AttachmentDataUtilsMixin {
    @ModifyArg(method = "getDamageWithAttachment", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentPropertyManager;eval(Ljava/util/List;D)D"), index = 1)
    private static double tacztweaks$getDamageWithAttachment$damageModifier(double base) {
        return Config.Modifiers.Damage.INSTANCE.eval(base);
    }

    @ModifyArg(method = "getHeadshotMultiplier", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentPropertyManager;eval(Ljava/util/List;D)D"), index = 1)
    private static double tacztweaks$getHeadshotMultiplier$headshotModifier(double base) {
        return Config.Modifiers.Headshot.INSTANCE.eval(base);
    }

    @ModifyArg(method = "getArmorIgnoreWithAttachment", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/modifier/AttachmentPropertyManager;eval(Ljava/util/List;D)D"), index = 1)
    private static double tacztweaks$getArmorIgnoreWithAttachment$armorIgnoreModifier(double base) {
        return Config.Modifiers.ArmorIgnore.INSTANCE.eval(base);
    }
}
