package me.muksc.tacztweaks.mixin.feature.gameplay.handling.reload_interrupts_shooting;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.entity.shooter.LivingEntityReload;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.index.CommonGunIndex;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntityReload.class, remap = false)
public abstract class LivingEntityReloadMixin {
    @Shadow @Final private ShooterDataHolder data;

    //? if >=1.21.11 {
    /*@WrapMethod(method = "reloadWithIndex")
    private void tacztweaks$reload$allowReloadWhileShoot(
        AbstractGunItem gunItem,
        ItemStack currentGunItem,
        CommonGunIndex gunIndex,
        Operation<Void> original
    ) {
        if (!Config.Gameplay.Handling.reloadInterruptsShooting()) {
            original.call(gunItem, currentGunItem, gunIndex);
            return;
        }
        long previous = data.shootTimestamp;
        data.shootTimestamp = Long.MIN_VALUE / 4;
        try {
            original.call(gunItem, currentGunItem, gunIndex);
        } finally {
            data.shootTimestamp = previous;
        }
    }
    *///?} else {
    @ModifyExpressionValue(method = "lambda$reload$0", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/shooter/LivingEntityShoot;getShootCoolDown()J"))
    private long tacztweaks$reload$allowReloadWhileShoot(long original) {
        return Config.Gameplay.Handling.reloadInterruptsShooting() ? 0L : original;
    }
    //?}
}
