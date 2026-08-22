package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.client.gui.components.GunPackList;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Forces the "filter by held item" option of the gun smith table to stay active.
 */
@Mixin(value = GunPackList.class, remap = false)
public abstract class GunPackListMixin {
    @ModifyReturnValue(method = "isByHandSelected", at = @At("RETURN"))
    private boolean tacztweaks$isByHandSelected$alwaysActive(boolean original) {
        if (!Config.Tweaks.INSTANCE.alwaysFilterByHand()) return original;
        return true;
    }
}
