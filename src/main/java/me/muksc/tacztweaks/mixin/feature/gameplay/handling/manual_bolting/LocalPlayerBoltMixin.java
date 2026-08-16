package me.muksc.tacztweaks.mixin.feature.gameplay.handling.manual_bolting;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.LocalPlayerBolt;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.config.Config.Gameplay.Handling.EManualBoltingType;
import me.muksc.tacztweaks.mixininterface.feature.gameplay.handling.manual_bolting.ManualBoltingData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerBolt.class, remap = false)
public abstract class LocalPlayerBoltMixin {
    @Shadow @Final private LocalPlayer player;
    @Shadow @Final private LocalPlayerDataHolder data;

    @Shadow public abstract void bolt();

    /** Replace the complete auto-bolt transaction when manual bolting is enabled. */
    @Inject(method = "tickAutoBolt", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$tickAutoBolt$manualBolting(CallbackInfo ci) {
        if (Config.Gameplay.Handling.manualBolting() == EManualBoltingType.DISABLED) return;
        ci.cancel();

        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            data.isBolting = false;
            return;
        }

        ManualBoltingData ext = ManualBoltingData.of(data);
        if (ext.tacztweaks$getBoltBeforeReload()) {
            bolt();
        }
        if (data.isBolting && iGun.hasBulletInBarrel(mainHandItem)) {
            data.isBolting = false;
        }
        if (ext.tacztweaks$getBoltBeforeReload() && !data.isBolting && !data.clientStateLock) {
            ext.tacztweaks$setBoltBeforeReload(false);
            IClientPlayerGunOperator.fromLocalPlayer(player).reload();
        }
    }
}
