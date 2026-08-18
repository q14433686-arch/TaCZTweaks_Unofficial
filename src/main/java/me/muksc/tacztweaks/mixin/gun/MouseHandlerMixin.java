package me.muksc.tacztweaks.mixin.gun;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.entity.IGunOperator;
import me.muksc.tacztweaks.client.CrawlPitchController;
import me.muksc.tacztweaks.client.input.ReduceSensitivityKey;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Input tweaks deliberately placed away from TaCZ's own LocalPlayer#turn wrapper:
 * sensitivity scales the outer turnPlayer call, while crawl pitch is clamped after all
 * TaCZ zoom processing. A client-tick fallback in TaCZTweaksClient covers non-mouse input.
 */
@Mixin(value = MouseHandler.class, priority = 1500)
public abstract class MouseHandlerMixin {
    @WrapOperation(
        method = "handleAccumulatedMovement",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;turnPlayer(D)V")
    )
    private void tacztweaks$handleAccumulatedMovement$reduceSensitivity(
        MouseHandler instance,
        double sensitivity,
        Operation<Void> original
    ) {
        double scaled = sensitivity;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (ReduceSensitivityKey.KEY.isDown()
                || (Config.Gun.INSTANCE.tiltGunKeyTriggersReduceSensitivity() && TiltGunKey.isActive(player)))) {
            double multiplier = Config.Gun.INSTANCE.reduceSensitivityKeyMultiplier();
            if (Config.Gun.INSTANCE.disableReduceSensitivityKeyWhileAiming()) {
                multiplier = 1 + (multiplier - 1)
                    * (1 - IGunOperator.fromLivingEntity(player).getSynAimingProgress());
            }
            scaled = sensitivity * multiplier;
        }
        original.call(instance, scaled);
    }

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void tacztweaks$turnPlayer$clampCrawlPitch(double sensitivity, CallbackInfo ci) {
        CrawlPitchController.apply(Minecraft.getInstance().player);
    }
}
