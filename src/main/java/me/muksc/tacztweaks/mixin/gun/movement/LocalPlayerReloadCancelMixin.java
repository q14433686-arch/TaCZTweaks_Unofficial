package me.muksc.tacztweaks.mixin.gun.movement;

import com.tacz.guns.client.gameplay.LocalPlayerReload;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.core.SprintReloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents TaCZ's sprint-input hook from cancelling reload when the option is enabled. */
@Mixin(value = LocalPlayerReload.class, remap = false)
public abstract class LocalPlayerReloadCancelMixin {
    @Inject(method = "cancelReload", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$cancelReload$sprintWhileReloading(CallbackInfo ci) {
        if (Config.Gun.INSTANCE.sprintWhileReloading() && SprintReloadContext.isApplyingSprint()) {
            ci.cancel();
        }
    }
}
