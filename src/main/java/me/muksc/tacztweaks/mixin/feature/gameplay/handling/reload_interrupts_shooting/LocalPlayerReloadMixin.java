package me.muksc.tacztweaks.mixin.feature.gameplay.handling.reload_interrupts_shooting;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gameplay.LocalPlayerReload;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.core.extension.TaCZExt;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LocalPlayerReload.class, remap = false)
public abstract class LocalPlayerReloadMixin {
    @Shadow @Final private LocalPlayerDataHolder data;

    //? if >=1.21.11 {
    /*@WrapMethod(method = "reloadWithDisplay")
    private void tacztweaks$reload$reloadInterruptsShoot(
        AbstractGunItem gunItem,
        GunDisplayInstance display,
        GunData gunData,
        ItemStack mainHandItem,
        Operation<Void> original
    ) {
        if (!Config.Gameplay.Handling.reloadInterruptsShooting()) {
            original.call(gunItem, display, gunData, mainHandItem);
            return;
        }
        boolean previous = data.clientStateLock;
        boolean temporary = TaCZExt.getClientStateLockExcludingShoot(data);
        data.clientStateLock = temporary;
        try {
            original.call(gunItem, display, gunData, mainHandItem);
        } finally {
            // Preserve a new reload lock, but restore a pre-existing shoot lock if reload exited early.
            if (data.clientStateLock == temporary) data.clientStateLock = previous;
        }
    }
    *///?} else {
    @WrapOperation(method = "lambda$reload$2", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lcom/tacz/guns/client/gameplay/LocalPlayerDataHolder;clientStateLock:Z"))
    private boolean tacztweaks$reload$reloadInterruptsShoot(LocalPlayerDataHolder instance, Operation<Boolean> original) {
        return Config.Gameplay.Handling.reloadInterruptsShooting()
            ? TaCZExt.getClientStateLockExcludingShoot(instance)
            : original.call(instance);
    }
    //?}
}
