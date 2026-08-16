package me.muksc.tacztweaks.mixin.feature.gameplay.handling.manual_bolting;

import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gameplay.LocalPlayerReload;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.mixininterface.feature.gameplay.handling.manual_bolting.ManualBoltingData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerReload.class, remap = false)
public abstract class LocalPlayerReloadMixin {
    @Shadow @Final private LocalPlayerDataHolder data;
    @Shadow @Final private LocalPlayer player;

    //? if >=1.21.11 {
    /*@Inject(method = "reloadWithDisplay", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$reload$manualBolting$boltBeforeReload(
        AbstractGunItem gunItem,
        GunDisplayInstance display,
        GunData gunData,
        ItemStack mainHandItem,
        CallbackInfo ci
    ) {
        if (gunItem.useInventoryAmmo(mainHandItem)) return;
        if (data.clientStateLock) return;
        if (System.currentTimeMillis() - data.clientShootTimestamp < 100) return;
        boolean canReload = gunItem.canReload(player, mainHandItem);
        if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && !canReload) return;
        if (shouldBoltBeforeReload(gunItem, gunData, mainHandItem)) ci.cancel();
    }
    *///?} else {
    @Inject(method = "lambda$reload$2", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/gameplay/LocalPlayerDataHolder;lockState(Ljava/util/function/Predicate;)V"), cancellable = true)
    private void tacztweaks$reload$manualBolting$boltBeforeReload(
        CallbackInfo ci,
        @Local(argsOnly = true) ItemStack mainHandItem,
        @Local(argsOnly = true) AbstractGunItem gunItem
    ) {
        ClientGunIndex index = TimelessAPI.getClientGunIndex(gunItem.getGunId(mainHandItem)).orElse(null);
        if (index != null && index.getGunData() != null && shouldBoltBeforeReload(gunItem, index.getGunData(), mainHandItem)) {
            ci.cancel();
        }
    }
    //?}

    private boolean shouldBoltBeforeReload(AbstractGunItem gunItem, GunData gunData, ItemStack mainHandItem) {
        if (Config.Gameplay.Handling.manualBolting() == Config.Gameplay.Handling.EManualBoltingType.DISABLED) return false;
        ManualBoltingData ext = ManualBoltingData.of(data);
        boolean shouldBolt = ext.tacztweaks$getBoltBeforeReload();
        if (!shouldBolt) {
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
            Bolt boltType = gunData.getBolt();
            boolean useInventoryAmmo = gunItem.useInventoryAmmo(mainHandItem);
            boolean hasAmmoInBarrel = gunItem.hasBulletInBarrel(mainHandItem) && boltType != Bolt.OPEN_BOLT;
            boolean hasInventoryAmmo = gunItem.hasInventoryAmmo(player, mainHandItem, gunOperator.needCheckAmmo());
            boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo || !useInventoryAmmo && gunItem.getCurrentAmmoCount(mainHandItem) < 1;
            shouldBolt = boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel && !noAmmo;
        }
        if (shouldBolt) ext.tacztweaks$setBoltBeforeReload(true);
        return shouldBolt;
    }
}
