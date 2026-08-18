package me.muksc.tacztweaks.mixin.gun.movement;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.muksc.tacztweaks.core.SprintReloadContext;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Opens a scope around the same sprint setter wrapped by TaCZ. Priority 1500 makes this
 * wrapper outermost, so the TaCZ cancellation call can identify sprint-input context.
 */
@Mixin(value = LocalPlayer.class, priority = 1500)
public abstract class LocalPlayerSprintReloadMixin {
    @WrapOperation(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V"
        )
    )
    private void tacztweaks$aiStep$scopeSprintReload(
        LocalPlayer player,
        boolean sprinting,
        Operation<Void> original
    ) {
        SprintReloadContext.run(() -> original.call(player, sprinting));
    }
}
