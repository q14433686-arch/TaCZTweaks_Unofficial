package me.muksc.tacztweaks.mixin.feature.disarm;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import me.muksc.tacztweaks.feature.disarm.DisarmManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientMessageRefitGun.class, remap = false)
public abstract class ClientMessageRefitGunMixin {
    private static boolean tacztweaks$handle$disarm(
        boolean original,
        @Local ServerPlayer player
    ) {
        return original || DisarmManager.shouldDisarm(player);
    }
}
