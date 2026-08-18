package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.tacz.guns.client.gui.components.GunPackList;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Forces the "filter by held item" option of the gun smith table to stay active.
 */
@Mixin(value = GunPackList.class, remap = false)
public abstract class GunPackListMixin {
    @WrapWithCondition(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/client/gui/components/GunPackList;addEntry(Lnet/minecraft/client/gui/components/AbstractSelectionList$Entry;)I",
            ordinal = 1,
            remap = true
        )
    )
    // AbstractSelectionList.Entry is protected; @Coerce keeps the handler compatible
    // with the invocation descriptor without illegally naming that nested type here.
    private boolean tacztweaks$init$hideByHandFilter(
        GunPackList instance,
        @Coerce Object entry
    ) {
        return !Config.Tweaks.INSTANCE.alwaysFilterByHand();
    }

    @ModifyReturnValue(method = "isByHandSelected", at = @At("RETURN"))
    private boolean tacztweaks$isByHandSelected$alwaysActive(boolean original) {
        if (!Config.Tweaks.INSTANCE.alwaysFilterByHand()) return original;
        return true;
    }
}
