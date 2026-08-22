package me.muksc.tacztweaks.mixin.gun;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunAnimationStateContext.class, remap = false)
public abstract class GunAnimationStateContextMixin {
    // NOTE: upstream targeted `lambda$shouldSlide$18`; the 26.2 refabricated port renumbered
    // the lambda to `lambda$shouldSlide$0`; the 1.21.11 port recompiled it as `$16`.
    @WrapOperation(method = "lambda$shouldSlide$16", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isCrouching()Z", remap = true))
    private boolean tacztweaks$shouldSlide$tiltGun(Entity instance, Operation<Boolean> original) {
        if (Config.Crawl.INSTANCE.tiltGun() == Config.Crawl.ETiltGun.NEVER && instance.isVisuallyCrawling()) return false;
        boolean crouch = !Config.Tweaks.INSTANCE.betterGunTilt() && original.call(instance);
        boolean tiltGunKey = (Config.Gun.INSTANCE.tiltGunKeyCancelsSprint() || !instance.isSprinting()) && TiltGunKey.KEY.isDown();
        boolean crawlTilt = Config.Crawl.INSTANCE.tiltGun() == Config.Crawl.ETiltGun.ALWAYS && instance.isVisuallyCrawling();
        return crouch || tiltGunKey || crawlTilt;
    }
}
