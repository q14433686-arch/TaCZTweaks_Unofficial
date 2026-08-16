package me.muksc.tacztweaks.mixin.feature.keyactions.unload;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.mixininterface.feature.keyactions.unload.UnloadableGun;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

//? if forge {
import net.minecraftforge.items.ItemHandlerHelper;
//?} else if neoforge {
/*import net.neoforged.neoforge.items.ItemHandlerHelper;
*///?} else if fabric {
/*import cn.sh1rocu.tacz.util.itemhandler.ItemHandlerHelper;
*///?}

@Mixin(value = AbstractGunItem.class, remap = false)
public abstract class AbstractGunItemMixin implements UnloadableGun {
    /**
     * A dedicated unload implementation avoids changing TaCZ's attachment-refit dropAllAmmo path
     * and avoids all compiler-generated Optional lambdas. Magazine and optional chamber rounds are
     * returned in correctly bounded stacks; creative and fuel weapons are only cleared.
     */
    @Override
    public void tacztweaks$unload(Player player, ItemStack gunItem) {
        AbstractGunItem gun = AbstractGunItem.class.cast(this);
        if (gun.useInventoryAmmo(gunItem)) return;

        int magazineRounds = gun.getCurrentAmmoCount(gunItem);
        boolean unloadChamber = Config.KeyActions.Unload.unloadRoundInChamber()
            && gun.hasBulletInBarrel(gunItem);
        if (magazineRounds <= 0 && !unloadChamber) return;

        TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem)).ifPresent(gunIndex -> {
            FeedType feedType = gunIndex.getGunData().getReloadData().getType();
            int rounds = Math.max(magazineRounds, 0) + (unloadChamber ? 1 : 0);

            if (gun.useDummyAmmo(gunItem)) {
                gun.setCurrentAmmoCount(gunItem, 0);
                if (unloadChamber) gun.setBulletInBarrel(gunItem, false);
                if (feedType != FeedType.FUEL) gun.addDummyAmmoAmount(gunItem, rounds);
                return;
            }

            if (player.isCreative() || feedType == FeedType.FUEL) {
                gun.setCurrentAmmoCount(gunItem, 0);
                if (unloadChamber) gun.setBulletInBarrel(gunItem, false);
                return;
            }

            TimelessAPI.getCommonAmmoIndex(gunIndex.getGunData().getAmmoId()).ifPresent(ammoIndex -> {
                int stackSize = Math.max(ammoIndex.getStackSize(), 1);
                int remaining = rounds;
                while (remaining > 0) {
                    int count = Math.min(remaining, stackSize);
                    ItemStack ammo = AmmoItemBuilder.create()
                        .setId(gunIndex.getGunData().getAmmoId())
                        .setCount(count)
                        .build();
                    ItemHandlerHelper.giveItemToPlayer(player, ammo);
                    remaining -= count;
                }
                gun.setCurrentAmmoCount(gunItem, 0);
                if (unloadChamber) gun.setBulletInBarrel(gunItem, false);
            });
        });
    }
}
