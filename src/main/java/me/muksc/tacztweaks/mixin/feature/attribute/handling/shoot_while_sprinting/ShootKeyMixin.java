package me.muksc.tacztweaks.mixin.feature.attribute.handling.shoot_while_sprinting;

import com.tacz.guns.client.gameplay.LocalPlayerSprint;
import com.tacz.guns.client.input.ShootKey;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShootKey.class, remap = false)
public abstract class ShootKeyMixin {
    /** Override the final sprint-stop decision at the stable autoShoot boundary. */
    @Inject(method = "autoShoot", at = @At("TAIL"))
    private static void tacztweaks$autoShoot$attribute$handling$shootWhileSprinting(
        Minecraft minecraft,
        boolean isPhaseEnd,
        CallbackInfo ci
    ) {
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        double value = player.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOT_WHILE_SPRINTING));
        if (value > 0.0) LocalPlayerSprint.stopSprint = false;
    }
}
