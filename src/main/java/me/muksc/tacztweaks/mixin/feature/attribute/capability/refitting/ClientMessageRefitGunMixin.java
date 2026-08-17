package me.muksc.tacztweaks.mixin.feature.attribute.capability.refitting;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientMessageRefitGun.class, remap = false)
public abstract class ClientMessageRefitGunMixin {
    private static boolean tacztweaks$handle$attribute$capability$refitting(
        boolean original,
        @Local ServerPlayer player
    ) {
        double value = player.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.REFITTING));
        return original || value <= 0.0;
    }
}
