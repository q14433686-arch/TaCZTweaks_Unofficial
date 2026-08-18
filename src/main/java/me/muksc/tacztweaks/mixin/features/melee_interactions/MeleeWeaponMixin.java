package me.muksc.tacztweaks.mixin.features.melee_interactions;

import me.muksc.tacztweaks.data.manager.MeleeInteractionManager;
import me.xjqsh.lrtactical.api.item.IMeleeWeapon;
import me.xjqsh.lrtactical.api.melee.MeleeAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LRTactical is bundled in the unofficial TaCZ jar. {@code performAttack} returns the
 * hit count; a miss is forwarded to the same block-break rules as gun bayonets.
 */
@Mixin(value = IMeleeWeapon.class, remap = false)
public interface MeleeWeaponMixin {
    @Inject(method = "performAttack", at = @At("RETURN"))
    private void tacztweaks$performAttack$handleMiss(
        ServerPlayer player,
        ItemStack stack,
        MeleeAction action,
        Vec3 from,
        Vec3 to,
        CallbackInfoReturnable<Integer> cir
    ) {
        Integer hits = cir.getReturnValue();
        if (hits != null && hits > 0) return;
        double reach = from.distanceTo(to);
        if (reach < 1.0) reach = 3.0;
        MeleeInteractionManager.INSTANCE.handleBlockInteraction(player, reach, 1.0F);
    }
}
