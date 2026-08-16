package me.muksc.tacztweaks.mixin.feature.attribute.capability.shooting;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayerShoot.class, remap = false)
public abstract class LocalPlayerShootMixin {
    @Shadow @Final private LocalPlayer player;

    @Inject(method = "shoot", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$shoot$attribute$capability$shooting(CallbackInfoReturnable<ShootResult> cir) {
        double value = player.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOTING));
        if (value <= 0.0) cir.setReturnValue(ShootResult.FORGE_EVENT_CANCEL);
    }
}