package me.muksc.tacztweaks.mixin.gun.movement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gameplay.LocalPlayerReload;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.mixin.accessor.LocalPlayerShootAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LocalPlayerReload.class, remap = false)
public abstract class LocalPlayerReloadMixin {
    @Shadow
    @Final
    private LocalPlayer player;

    @Shadow
    @Final
    private LocalPlayerDataHolder data;

    /**
     * When shooting and reloading overlap, select the empty-reload animation only if both
     * chamber and magazine are empty. R2 otherwise checks only the chamber on closed-bolt
     * guns, which was the behavior repaired by upstream's old doReload injection.
     */
    @ModifyExpressionValue(
        method = "triggerClientReloadAnimation",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/IGun;hasBulletInBarrel(Lnet/minecraft/world/item/ItemStack;)Z"
        )
    )
    private boolean tacztweaks$triggerClientReloadAnimation$countMagazineAmmo(
        boolean chambered,
        @Local(argsOnly = true) IGun gun,
        @Local(argsOnly = true) ItemStack mainHandItem
    ) {
        if (!Config.Gun.INSTANCE.reloadWhileShooting()) return chambered;
        return chambered || gun.getCurrentAmmoCount(mainHandItem) > 0;
    }

    // NOTE: upstream targeted `lambda$reload$2`; the 26.1.2 refabricated port renamed the
    // reload body to the stable hook `reloadWithDisplay`.
    @ModifyExpressionValue(method = "reloadWithDisplay", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lcom/tacz/guns/client/gameplay/LocalPlayerDataHolder;clientStateLock:Z"))
    private boolean tacztweaks$reload$allowReloadWhileShoot(boolean original, @Local(argsOnly = true) ItemStack mainHandItem, @Local(argsOnly = true) AbstractGunItem gunItem) {
        if (!Config.Gun.INSTANCE.reloadWhileShooting()) return original;
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        if (data.lockedCondition == LocalPlayerShootAccessor.getShootLockedCondition()) return false;
        if (data.lockedCondition == null && !operator.getSynReloadState().getStateType().isReloading()
                && operator.getSynDrawCoolDown() <= 0
                && !operator.getSynIsBolting()
                && operator.getSynMeleeCoolDown() <= 0L) return false;
        return original;
    }
}
