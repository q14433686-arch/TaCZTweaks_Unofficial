package me.muksc.tacztweaks.mixin.feature.gameplay.behaviour.reload_discards_magazine;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModernKineticGunItem.class, remap = false)
public abstract class ModernKineticGunItemMixin extends AbstractGunItem {
    protected ModernKineticGunItemMixin(Properties pProperties) {
        super(pProperties);
    }

    /** Use the stable public transaction boundary; no script Optional or local capture is needed. */
    @Inject(method = "startReload", at = @At("HEAD"))
    private void tacztweaks$startReload$reloadDiscardsMagazine(
        ShooterDataHolder dataHolder,
        ItemStack gunItem,
        LivingEntity shooter,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Config.Gameplay.Behaviour.magazineStyleReloading()) return;
        String gunId = getGunId(gunItem).toString();
        if (Config.Gameplay.Behaviour.magazineStyleReloadingExclusions().contains(gunId)) return;
        setCurrentAmmoCount(gunItem, 0);
    }
}
